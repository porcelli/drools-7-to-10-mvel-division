package example;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DivisionTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void verifiesDivisionInBothModels(boolean executable) throws ClassNotFoundException {
        KieServices kie = KieServices.Factory.get();
        var files = kie.newKieFileSystem();
        files.write("src/main/resources/division.drl",
                kie.getResources().newClassPathResource("division.drl"));

        KieBuilder builder = kie.newKieBuilder(files);
        if (executable) {
            var model = Class.forName(System.getProperty("model.project"))
                    .asSubclass(KieBuilder.ProjectType.class);
            builder.buildAll(model);
        } else {
            builder.buildAll();
        }
        assertFalse(builder.getResults().hasMessages(Message.Level.ERROR),
                () -> builder.getResults().toString());

        var releaseId = builder.getKieModule().getReleaseId();
        KieContainer container = kie.newKieContainer(releaseId);
        try {
            check(container, executable, 7);
            check(container, executable, 8);
        } finally {
            container.dispose();
            kie.getRepository().removeKieModule(releaseId);
        }
    }

    private void check(KieContainer container, boolean executable, int numerator) {
        Map<String, Object> results = new LinkedHashMap<>();
        KieSession session = container.newKieSession();
        try {
            session.setGlobal("results", results);
            session.insert(new DivisionInput(numerator, 2));
            session.fireAllRules();
        } finally {
            session.dispose();
        }
        boolean classicDivision = !executable || "drools7".equalsIgnoreCase(System.getProperty("drools.mvel.compatibility"));
        assertEquals(expectedQuotient(classicDivision, 7), results.get("literal"), "literal consequence");
        Number quotient = expectedQuotient(classicDivision, numerator);
        assertEquals(quotient, results.get("primitive"), "int / int consequence");
        assertEquals(quotient, results.get("boxed"), "int / Integer consequence");
        assertEquals(numerator == 8, results.containsKey("aboveThreshold"), "eval threshold");
        System.out.printf("%s | %s | %d / 2 | literal 7 / 2 = %s | int/int = %s"
                        + " | int/Integer = %s | above 3 = %s%n",
                System.getProperty("comparison.build"), executable ? "executable" : "classic",
                numerator, describe(results.get("literal")), describe(results.get("primitive")),
                describe(results.get("boxed")), results.containsKey("aboveThreshold"));
    }

    private Number expectedQuotient(boolean classicDivision, int numerator) {
        if (classicDivision) {
            return numerator / 2.0;
        }
        return numerator / 2;
    }

    private String describe(Object value) {
        return value + " [" + value.getClass().getSimpleName() + "]";
    }
}
