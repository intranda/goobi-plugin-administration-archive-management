package de.intranda.goobi.plugins.model;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import de.a9d3.testing.checks.CopyConstructorCheck;
import de.a9d3.testing.checks.DefensiveCopyingCheck;
import de.a9d3.testing.checks.EmptyCollectionCheck;
import de.a9d3.testing.checks.GetterIsSetterCheck;
import de.a9d3.testing.checks.HashcodeAndEqualsCheck;
import de.a9d3.testing.checks.PublicVariableCheck;
import de.a9d3.testing.executer.SingleThreadExecutor;

public class DuplicationParameterTest {

    @Test
    public void baseTest() {
        SingleThreadExecutor executor = new SingleThreadExecutor();

        assertTrue(executor.execute(DuplicationParameter.class, Arrays.asList(
                new CopyConstructorCheck(), new DefensiveCopyingCheck(),
                new EmptyCollectionCheck(), new GetterIsSetterCheck(),
                new HashcodeAndEqualsCheck(), new PublicVariableCheck())));
    }
}
