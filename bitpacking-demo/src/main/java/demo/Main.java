package demo;

public class Main {
    public static void main(String[] args) {
        System.out.println("hi");
        short packed = HausSpecBit.pack(10, 2, true);
        System.out.println("packed "+packed);
    }
}
