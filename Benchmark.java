import com.google.protobuf.ByteString;

public class Benchmark {
    private static final byte DEFAULT_SPAN_ID_BYTE = 0x11;
    private static final ByteString DEFAULT_SPAN_ID = ByteString.copyFrom(new byte[] {
            DEFAULT_SPAN_ID_BYTE, DEFAULT_SPAN_ID_BYTE, DEFAULT_SPAN_ID_BYTE, DEFAULT_SPAN_ID_BYTE,
            DEFAULT_SPAN_ID_BYTE, DEFAULT_SPAN_ID_BYTE, DEFAULT_SPAN_ID_BYTE, DEFAULT_SPAN_ID_BYTE
    });

    public static void main(String[] args) throws Exception {
        // warm up
        for (int i = 0; i < 100000; i++) {
            baseline();
            optimized();
        }

        long t1 = System.nanoTime();
        for (int i = 0; i < 10000000; i++) {
            baseline();
        }
        long t2 = System.nanoTime();
        for (int i = 0; i < 10000000; i++) {
            optimized();
        }
        long t3 = System.nanoTime();

        System.out.println("Baseline: " + (t2 - t1) / 1000000 + " ms");
        System.out.println("Optimized: " + (t3 - t2) / 1000000 + " ms");
    }

    public static void baseline() {
        byte[] finalParentSpanIdBytes =
          new byte[] {
            DEFAULT_SPAN_ID_BYTE, DEFAULT_SPAN_ID_BYTE, DEFAULT_SPAN_ID_BYTE, DEFAULT_SPAN_ID_BYTE,
            DEFAULT_SPAN_ID_BYTE, DEFAULT_SPAN_ID_BYTE, DEFAULT_SPAN_ID_BYTE, DEFAULT_SPAN_ID_BYTE
          };
        ByteString b = ByteString.copyFrom(finalParentSpanIdBytes);
    }

    public static void optimized() {
        ByteString b = DEFAULT_SPAN_ID;
    }
}
