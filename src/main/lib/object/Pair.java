import lib.data.Pair;
import lib.data.Triple;

interface Base {
    // Java interfaces do not have constructors
}

interface Position {
    // Return type should be a class, e.g., Point or similar
    Pair<Integer, Integer> getPosition();
}

interface Size {
    Pair<Integer, Integer> getSize();
}

interface Color {
    // Return type should be a class, e.g., Color or similar
    Triple<Integer, Integer, Integer> getColor();
}

interface Named {
    String getName();
    void setName(String name);
}

// You need to define Pair and Triple classes or use java.awt.Point and java.awt.Color if suitable

