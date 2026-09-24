package pl.solutiontabs.rules;

import org.junit.jupiter.api.Test;
import java.awt.Color;
import static org.junit.jupiter.api.Assertions.*;

class RuleEngineTest {
    @Test void parsesHexColor() { assertEquals(new Color(0x4E9AEB), RuleEngine.parseColor("#4E9AEB")); }
    @Test void invalidColorIsIgnored() { assertNull(RuleEngine.parseColor("blue-ish")); }
}
