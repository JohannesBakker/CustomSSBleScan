package systems.kestrel.bluetooth_le.services;

/**
 * Created by dev on 6/27/2016.
 */
public interface onBandActionListener {
    void completed(Object object);
    void failed(int code, String message);

    void reportStatus(Object param);
}
