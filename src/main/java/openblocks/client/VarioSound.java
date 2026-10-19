package openblocks.client;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CompletableFuture;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.sounds.SoundSource;
import openblocks.registry.OBSounds;
import org.lwjgl.BufferUtils;

/**
 * Port of legacy {@code BeepGenerator}: instead of an own {@code SourceDataLine} the tone is a streamed sound of Minecraft's
 * sound engine (the file in {@code sounds.json} is never read), so it follows the sound volume and pauses with the game.
 * Frequencies are set from the client thread and read on the sound thread.
 */
public class VarioSound extends AbstractTickableSoundInstance {

	private static final int SAMPLE_RATE = 44100;

	// the engine queues four buffers, so this is also the delay of the tone
	private static final int SAMPLES_PER_BUFFER = SAMPLE_RATE / 10;

	private static final int BYTES_PER_SAMPLE = 2;

	private static final int BYTES_PER_BUFFER = BYTES_PER_SAMPLE * SAMPLES_PER_BUFFER;

	private static final double BUFFER_DURATION = (double)SAMPLES_PER_BUFFER / SAMPLE_RATE;

	// legacy: 50 Hz per 1/8 s
	private static final double FREQUENCY_MAX_CHANGE_PER_BUFFER = 400 * BUFFER_DURATION;

	// legacy volume 20 << 8
	private static final double AMPLITUDE = 5120;

	private volatile double targetToneFrequency;
	private volatile int samplesPerBeep;

	public VarioSound() {
		super(OBSounds.VARIO.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
		relative = true;
		attenuation = SoundInstance.Attenuation.NONE;
		volume = Vario.getVolumeFactor();
	}

	public void setFrequencies(double toneFrequency, double beepFrequency) {
		targetToneFrequency = toneFrequency;
		samplesPerBeep = beepFrequency == 0? 0 : (int)(SAMPLE_RATE / beepFrequency);
	}

	@Override
	public void tick() {
		volume = Vario.getVolumeFactor();
	}

	@Override
	public boolean canStartSilent() {
		return true;
	}

	@Override
	public CompletableFuture<AudioStream> getStream(SoundBufferLibrary soundBuffers, Sound sound, boolean looping) {
		return CompletableFuture.completedFuture(new ToneStream());
	}

	private class ToneStream implements AudioStream {

		private static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 8 * BYTES_PER_SAMPLE, 1, true, false);

		private double toneFrequency;
		private double wavePhase;
		private int beepPhase;

		@Override
		public AudioFormat getFormat() {
			return FORMAT;
		}

		/**
		 * Always a full buffer (silent if there is no tone): when the queue runs dry, the engine ends the sound.
		 */
		@Override
		public ByteBuffer read(int size) {
			final double target = targetToneFrequency;
			final double lastToneFrequency;

			if (toneFrequency == 0 || target == 0) {
				lastToneFrequency = target;
				toneFrequency = target;
			} else {
				lastToneFrequency = toneFrequency;
				toneFrequency += Math.clamp(target - toneFrequency, -FREQUENCY_MAX_CHANGE_PER_BUFFER, FREQUENCY_MAX_CHANGE_PER_BUFFER);
			}

			final ByteBuffer buffer = BufferUtils.createByteBuffer(BYTES_PER_BUFFER).order(ByteOrder.LITTLE_ENDIAN);
			if (lastToneFrequency == 0 && toneFrequency == 0) return buffer;

			// linear sweep between the two frequencies ("chirp")
			final double f0 = lastToneFrequency;
			final double k = (toneFrequency - f0) / BUFFER_DURATION;
			final int beepLength = samplesPerBeep;

			for (int i = 0; i < SAMPLES_PER_BUFFER; i++) {
				final boolean audible = beepLength == 0 || beepPhase < beepLength;
				buffer.putShort(audible? (short)(AMPLITUDE * Math.sin(phase(f0, k, (double)i / SAMPLE_RATE))) : 0);

				if (beepLength != 0 && beepPhase++ >= 2 * beepLength) beepPhase = 0;
			}

			wavePhase = phase(f0, k, BUFFER_DURATION) % (2 * Math.PI);
			return buffer.flip();
		}

		private double phase(double f0, double k, double t) {
			return wavePhase + 2 * Math.PI * (f0 + k / 2 * t) * t;
		}

		@Override
		public void close() {}
	}
}
