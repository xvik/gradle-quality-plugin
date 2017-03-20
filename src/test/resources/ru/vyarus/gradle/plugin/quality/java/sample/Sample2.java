package sample;

import java.util.ArrayDeque;
import java.util.Deque;

public class Sample2 {

    private String sample;

    public Sample2(String sample) {
        this.sample = sample;
    }

    public static void main(String[] args) {
        final Deque res = new ArrayDeque();
        System.out.println(res);
    }
}