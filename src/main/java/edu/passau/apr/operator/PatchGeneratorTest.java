package edu.passau.apr.operator;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

import edu.passau.apr.model.Edit;
import edu.passau.apr.model.Patch;
import org.junit.jupiter.api.Test;

class PatchGeneratorTest {

    String source = """
        public class Calculator {
            public int sum(int... numbers) {
                int result = 0;
                result = 2;
                result = numbers.length;
                int i = 0;
                for (int num : numbers) {
                    result += num;
                    i++;
                }
                return result - i;
            }
        }
        """;
    int seed = 42;

    Random generateConstRandomMock(int constantValue) {
        return new Random(seed) {
            @Override
            public int nextInt(int bound) {
                return constantValue;
            }
            @Override
            public int nextInt() {
                return constantValue;
            }
        };
    }

    @Test
    void crossoverTest() {
        PatchGenerator generator = new PatchGenerator(source, null, 0, generateConstRandomMock(2));
        Patch p = new Patch(source, null);
        Patch q = new Patch(source, null);

        p.applyEdit(new Edit(Edit.Type.DELETE, 0, null));
        q.applyEdit(new Edit(Edit.Type.INSERT, 6, 1));

        System.out.println("Parent 1:");
        System.out.println(p.getCompilationUnit().toString());

        System.out.println("Parent 2:");
        System.out.println(q.getCompilationUnit().toString());

        System.out.println("-----------");

        var offspring = generator.crossover(p, q);
        Patch c = offspring.first();
        Patch d = offspring.second();

        System.out.println("Offspring 1:");
        System.out.println(c.getCompilationUnit().toString());

        System.out.println("Offspring 2:");
        System.out.println(d.getCompilationUnit().toString());


    }
}
