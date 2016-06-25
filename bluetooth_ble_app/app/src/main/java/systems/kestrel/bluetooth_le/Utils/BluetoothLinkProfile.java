package systems.kestrel.bluetooth_le.Utils;

/**
 * Created by ragsagar on 5/17/16.
 */
public enum BluetoothLinkProfile {
    US, Gulf, GulfHunting, GulfTesla;

    public int getNumber() {
        switch(this) {
            case US:
                return 1;
            case Gulf:
                return 4;
            case GulfHunting:
                return 5;
            case GulfTesla:
                return 6;
        }
        return 1;
    }

    public static BluetoothLinkProfile getProfile(int number) {
        if (number == US.getNumber()) {
            return US;
        } else if (number == Gulf.getNumber()) {
            return Gulf;
        } else if (number == GulfHunting.getNumber()) {
            return GulfHunting;
        } else if (number == GulfTesla.getNumber()) {
            return GulfTesla;
        } else {
            // default.
            return US;
        }
    }
}
