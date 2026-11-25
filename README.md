# MVEL division in Drools 7 and Drools 10

Why can `7 / 2` change from `3.5` to `3` when you migrate a Drools rule? In the `then` block of the DRL rule below, **classic MVEL returns `3.5` as a Java `Double`, and the executable model returns `3` as an `Integer`**. The same rule gives those results on both community releases.

| Community release | Classic MVEL | Executable model |
| --- | --- | --- |
| Drools 7.74.1.Final | `3.5` (`Double`) | `3` (`Integer`) |
| Drools 10.2.0 | `3.5` (`Double`) | `3` (`Integer`) |

If you're migrating from Drools 7 to Drools 10, check how each KJAR was built. Moving from classic MVEL on 7 to the executable model on 10 changes this rule’s answer. Keeping the execution model the same gives the same answer across the two releases.

## Run the Maven comparison

With JDK 17 and Maven 3.9.x installed, run this from the repository root.

```sh
mvn test
```

This runs four JUnit cases using public Maven dependencies. Each release runs the same rules twice, once with classic MVEL and once with the executable model. The tests print the values and their Java types and fail if either changes.

## The DRL rule behind the result

```drl
package example;

global java.util.Map results;

rule "literal division in a consequence"
    dialect "mvel"
when
then
    results.put("literal", 7 / 2);
end
```

The rule writes to a Java map so the test can inspect the value and its type. All four community runs use this DRL, including its `dialect "mvel"` declaration. The [JUnit test](src/test/java/example/DivisionTest.java) selects the execution model when it builds the rules.

## Does a division in a rule condition behave the same way?

The [DRL file](src/test/resources/division.drl) also tests a `when` condition, `eval($n / $d > 3)`, using integer fields from a [synthetic fact](src/test/java/example/DivisionInput.java). With inputs 7 and 2, the condition is **false** in both models on both releases. With 8 and 2, it is **true** in all four cases.

The tests also cover `int / int` and `int / Integer` in a consequence. With inputs 8 and 2, the values are numerically equal, but classic MVEL produces a `Double` (`4.0`) and the executable model produces an `Integer` (`4`). That distinction matters if the next step expects a particular Java type.

For more on rule behavior, support, and BPMN ruleflows during an upgrade, see the Aletyx article [Is Drools 7 Still Supported? Lessons From Migrating Hundreds of Thousands of Rules](https://aletyx.ai/blog/is-drools-7-still-supported/).

## Optional comparison with Aletyx Horizon

The [Horizon module](aletyx-horizon/pom.xml) uses the Aletyx BOM `10.2.1-aletyx-horizon-013`. The default `mvn test` command above runs only the two community releases. If you have Horizon repository access, pass your Maven settings file from outside the checkout.

```sh
# Run all three releases with their default settings
mvn -s /path/to/settings-horizon.xml -Paletyx test

# Run all three, enabling Drools 7 compatibility for Horizon
mvn -s /path/to/settings-horizon.xml -Paletyx,drools7-compat test

# Run just Horizon with its defaults
mvn -s /path/to/settings-horizon.xml -f aletyx-horizon/pom.xml test
```

For the literal rule above, the Horizon executable model returns `3` (`Integer`) by default. The `drools7-compat` profile sets `drools.mvel.compatibility=drools7` in the test JVM before the rules are built. With that profile, the executable model returns `3.5` (`Double`), matching classic MVEL. The threshold condition gives the same results with either configuration.

To try the comparison with your own rules, [request a Drools migration trial from Aletyx](https://aletyx.ai/free-trial/).
