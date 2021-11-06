package net.khasm.instrumentation;

import com.sun.tools.attach.VirtualMachine;

import java.lang.instrument.Instrumentation;

@SuppressWarnings("unused")
public class InstrumentationAgent {
    public static Instrumentation instrumentation;

    public static void premain(String s, Instrumentation instrumentation) {
        InstrumentationAgent.instrumentation = instrumentation;
    }

    public static void agentmain(String s, Instrumentation instrumentation) {
        premain(s, instrumentation);
    }

    // oh also the agent has to load itself from a psvm call
    public static void main(String[] args) throws Exception {
        String pid = args[0];
        String jar = args[1];
        VirtualMachine vm = null;
        try {
            vm = VirtualMachine.attach(pid);
            vm.loadAgent(jar);
        } finally {
            if (vm != null) vm.detach();
        }
    }
}
