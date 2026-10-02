package fr.poubone.att2.client.data;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StatEffectTextTest {
    @Test void capsMatchInventory() {
        assertEquals(14, StatCaps.maxBase("STR"));
        assertEquals(8, StatCaps.maxBase("HER"));
        assertEquals(14, StatCaps.maxBase("HUN"));
        assertEquals(0, StatCaps.maxBase("NOPE"));
    }

    @Test void strResSpdHerLucDar() {
        var bare = StatEffectText.current("STR", 4);
        assertEquals("screen.stat_upgrade.effect.str.weapon", bare.langKey());

        var str = StatEffectText.current("STR", 4, new StatEffectText.WeaponStats(400, 1600));
        assertEquals("screen.stat_upgrade.effect.str", str.langKey());
        assertEquals(53, ((Number) str.args()[0]).intValue());

        var res = StatEffectText.current("RES", 4);
        assertEquals("screen.stat_upgrade.effect.res", res.langKey());
        assertEquals(8, ((Number) res.args()[0]).intValue());
        assertEquals(24, ((Number) res.args()[1]).intValue());
        assertEquals("screen.stat_upgrade.effect.res.immune", StatEffectText.current("RES", 17).langKey());
        assertEquals("screen.stat_upgrade.effect.res.none", StatEffectText.current("RES", 0).langKey());

        assertEquals(20, ((Number) StatEffectText.current("HAS", 4).args()[0]).intValue());
        assertEquals(40, ((Number) StatEffectText.current("SPD", 4).args()[0]).intValue());
        assertEquals("0.4", StatEffectText.current("HER", 4).args()[0]);
        assertEquals("2.0", StatEffectText.current("LUC", 4).args()[0]);
        assertEquals(5, ((Number) StatEffectText.current("DAR", 4).args()[0]).intValue());
        assertEquals(1, ((Number) StatEffectText.current("DAR", 0).args()[0]).intValue());
        assertEquals(22, ((Number) StatEffectText.current("DAR", 10).args()[0]).intValue());
    }

    @Test void nextEmptyAtMaxBase() {
        assertTrue(StatEffectText.next("HER", 8, 8).isEmpty());
        Optional<StatEffectText.EffectView> n = StatEffectText.next("HER", 3, 3);
        assertTrue(n.isPresent());
        assertEquals("0.4", n.get().args()[0]); // tot+1 = 4 → 0.4 PV/s
    }

    @Test void hunTiers() {
        assertEquals("screen.stat_upgrade.effect.hun.none", StatEffectText.current("HUN", 3).langKey());
        assertEquals("screen.stat_upgrade.effect.hun.neutral", StatEffectText.current("HUN", 6).langKey());
        var zero = StatEffectText.current("HUN", 0);
        assertEquals("screen.stat_upgrade.effect.hun.hunger", zero.langKey());
        assertEquals(2, ((Number) zero.args()[0]).intValue());
        assertEquals(7.5, ((Number) zero.args()[1]).doubleValue(), 0.01);
        var sat = StatEffectText.current("HUN", 8);
        assertEquals("screen.stat_upgrade.effect.hun.sat", sat.langKey());
        assertEquals(1, ((Number) sat.args()[0]).intValue());
        assertEquals(125.0, ((Number) sat.args()[1]).doubleValue(), 0.01);
        var hi = StatEffectText.current("HUN", 14);
        assertEquals(2, ((Number) hi.args()[0]).intValue());
        assertEquals(56.0, ((Number) hi.args()[1]).doubleValue(), 0.01);
        var top = StatEffectText.current("HUN", 16);
        assertEquals(2, ((Number) top.args()[0]).intValue());
        assertEquals(40.0, ((Number) top.args()[1]).doubleValue(), 0.01);
    }
}
