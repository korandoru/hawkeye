package io.korandoru.hawkeye.command;

import picocli.CommandLine;

@CommandLine.Command(
        name = "hawkeye",
        version = CommandConstants.VERSION,
        mixinStandardHelpOptions = true
)
public class HawkEyeCommandMain implements Runnable {

    @Override
    public void run() {
        System.out.println("Hello HawkEye!");
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new HawkEyeCommandMain()).execute(args));
    }

}
