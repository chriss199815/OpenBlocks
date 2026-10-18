package openblocks.api;

/**
 * Entities carried by the crane magnet can implement this to refuse being released, e.g. while there is no room.
 */
public interface IMagnetAware {
	boolean canRelease();
}
