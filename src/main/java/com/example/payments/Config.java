package com.example.payments;

// --interval=N (seconds) and --port=N. Anything else throws with a usage line.
public final class Config {

    public final int intervalSeconds;
    public final int port;

    private Config(int intervalSeconds, int port) {
        this.intervalSeconds = intervalSeconds;
        this.port = port;
    }

    public static Config parse(String[] args) {
        int interval = 60;
        int port = 8080;
        for (String arg : args) {
            if (arg.startsWith("--interval=")) {
                interval = positiveInt(arg, "--interval");
            } else if (arg.startsWith("--port=")) {
                port = positiveInt(arg, "--port");
            } else {
                throw new IllegalArgumentException("unknown argument: " + arg + "\n" + USAGE);
            }
        }
        return new Config(interval, port);
    }

    private static int positiveInt(String arg, String name) {
        String value = arg.substring(arg.indexOf('=') + 1);
        int n;
        try {
            n = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " must be an integer, got '" + value + "'\n" + USAGE);
        }
        if (n <= 0) {
            throw new IllegalArgumentException(name + " must be positive, got " + n + "\n" + USAGE);
        }
        return n;
    }

    private static final String USAGE =
            "usage: java -jar payments.jar [--interval=N] [--port=N]   (defaults: 60s, port 8080)";
}
