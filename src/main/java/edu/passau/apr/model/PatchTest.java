package edu.passau.apr.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PatchTest {
    Patch p;

    @BeforeEach
    void setUp() {
        p = new Patch("""
                class A {
                    void m() {
                        int x = 1;
                    }
                }
                """,
                Map.of(
                        2, 12d,
                        3, 13d
                )
        );
    }

    @Test
    void __test() {

    }
}
