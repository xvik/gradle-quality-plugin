package sample;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayDeque;
import java.util.Deque;

@SuppressFBWarnings("NP_BOOLEAN_RETURN_NULL")
public class SBSuppressSample {

    private String sample;

    public SBSuppressSample(String sample) {
        this.sample = sample;
    }

    public static void main(String[] args) {
        final Deque res = new ArrayDeque();
        System.out.println(res);
    }

    public Boolean someting() {
        return null;
    }
}