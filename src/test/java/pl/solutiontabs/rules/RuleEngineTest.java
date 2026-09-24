package pl.solutiontabs.rules;

import org.junit.Test;
import java.awt.Color;
import static org.junit.Assert.*;

public class RuleEngineTest {
    @Test
    public void parsesHexColor() {
        assertEquals(new Color(0x4E9AEB), RuleEngine.parseColor("#4E9AEB"));
    }

    @Test
    public void invalidColorIsIgnored() {
        assertNull(RuleEngine.parseColor("blue-ish"));
    }
}
