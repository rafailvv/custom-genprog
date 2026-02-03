package edu.passau.apr.model;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import edu.passau.apr.util.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static edu.passau.apr.model.Edit.Type.DELETE;
import static edu.passau.apr.model.Edit.Type.INSERT;
import static edu.passau.apr.model.Edit.Type.SWAP;

/**
 * Represents a patch as a collection of edits.
 * A patch is applied to the buggy source code to create a candidate fix.
 */
public class Patch {
    private final CompilationUnit compilationUnit; // cache of the patched compilation unit
    private final Map<Node, Double> nodeWeightMap; // map of suspicious values for nodes
    private final List<Edit> edits = new ArrayList<>(); // list of edits applied one after another

    public Patch(CompilationUnit compilationUnit, Map<Node, Double> nodeWeightMap) {
        this.compilationUnit = compilationUnit.clone();
        this.nodeWeightMap = nodeWeightMap;
    }

    public Patch(String source, List<StatementWeight> nodeWeights) {
        this.nodeWeightMap = new HashMap<>();
        this.compilationUnit = StaticJavaParser.parse(source);
        this.compilationUnit.findAll(Statement.class).forEach(statement -> {
            int line = statement.getBegin().map(p -> p.line).orElse(-1);
            double weight = 0.0;
            for (var sw : nodeWeights) {
                if (sw.getLineNumber() == line) {
                    weight = sw.getWeight();
                    break;
                }
            }
            nodeWeightMap.put(statement, weight);
        });
    }

    public void doMutations(double mutationRate, Random random) {
        int i = 0;
        for (Statement stmt : compilationUnit.findAll(Statement.class)) {
            Double weight = nodeWeightMap.getOrDefault(stmt, 0.1); // assume 0.1 for modified statements
            if (random.nextDouble() < mutationRate && random.nextDouble() < weight) {
                var operation = choose(random, Edit.Type.values());
                switch (operation) {
                    case DELETE -> {
                        stmt.remove();
                        edits.add(new Edit(DELETE, i, null));
                    }
                    case INSERT -> {
                        var donorStmt = getDonorStatement(random);
                        if (donorStmt != null) {
                            int _i = i;
                            stmt.getParentNode().map(n -> (BlockStmt) n).ifPresent(parent -> {
                                parent.getStatements().addBefore(donorStmt.first(), stmt);
                                edits.add(new Edit(INSERT, _i, donorStmt.second()));
                            });
                        }
                    }
                    case SWAP -> {
                        var donorStmt = getDonorStatement(random);
                        if (donorStmt != null) {
                            stmt.replace(donorStmt.first().clone());
                            donorStmt.first().replace(stmt.clone());
                            edits.add(new Edit(SWAP, i, donorStmt.second()));
                        }
                    }
                }
            }

            i++;
        }
    }

    private Edit.Type choose(Random random, Edit.Type[] values) {
        int index = random.nextInt(values.length);
        return values[index];
    }


    private Pair<Statement, Integer> getDonorStatement(Random random) {
        List<Statement> statements = compilationUnit.findAll(Statement.class);
        if (statements.isEmpty()) {
            return null;
        }
        int index = random.nextInt(statements.size());
        return new Pair<>(statements.get(index), index);
    }

    public CompilationUnit getCompilationUnit() {
        return compilationUnit;
    }
}
