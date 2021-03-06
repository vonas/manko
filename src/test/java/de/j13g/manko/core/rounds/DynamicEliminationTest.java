package de.j13g.manko.core.rounds;

import de.j13g.manko.RoundTest;
import de.j13g.manko.core.Pairing;
import de.j13g.manko.core.TestEntrant;
import de.j13g.manko.core.exceptions.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static de.j13g.manko.Helper.assertSuppliesAll;
import static org.junit.jupiter.api.Assertions.*;

public class DynamicEliminationTest extends RoundTest {

    private final List<TestEntrant> entrants = Arrays.asList(
            first, second, third, fourth,
            createEntrant(), createEntrant(), createEntrant(),
            createEntrant(), createEntrant(), createEntrant()
    );

    private final TestEntrant winner = first;
    private final TestEntrant loser = second;

    private DynamicElimination<TestEntrant> emptyRound;
    private DynamicElimination<TestEntrant> oneEntrantRound;
    private DynamicElimination<TestEntrant> twoEntrantRound;
    private DynamicElimination<TestEntrant> multiEntrantRound;

    private DynamicElimination<TestEntrant> singlePairRound;
    private DynamicElimination<TestEntrant> singlePairFinishedRound;

    @BeforeEach
    void init() {
        emptyRound = new DynamicElimination<>();

        oneEntrantRound = new DynamicElimination<>();
        oneEntrantRound.addEntrant(first);

        twoEntrantRound = new DynamicElimination<>();
        twoEntrantRound.addEntrant(first);
        twoEntrantRound.addEntrant(second);

        multiEntrantRound = createMultiEntrantRound();

        singlePairRound = new DynamicElimination<>();
        singlePairRound.addEntrant(first);
        singlePairRound.addEntrant(second);
        assertDoesNotThrow(() -> singlePairRound.nextPairing());

        singlePairFinishedRound = createSinglePairFinishedRound();
    }

    private DynamicElimination<TestEntrant> createMultiEntrantRound() {
        DynamicElimination<TestEntrant> round = new DynamicElimination<>();
        for (TestEntrant entrant : entrants)
            round.addEntrant(entrant);
        return round;
    }

    private DynamicElimination<TestEntrant> createSinglePairFinishedRound() {
        DynamicElimination<TestEntrant> round = new DynamicElimination<>();
        round.addEntrant(first);
        round.addEntrant(second);
        assertDoesNotThrow(round::nextPairing);
        assertDoesNotThrow(() -> round.declareWinner(first));
        return round;
    }

    private Pairing<TestEntrant> getFinishedPairing(DynamicElimination<TestEntrant> round) {
        assertEquals(1, round.getFinishedPairings().size());
        return round.getFinishedPairings().iterator().next();
    }

    // addEntrant()

    @Test
    void emptyRound_addEntrant_returnsTrue() {
        assertTrue(emptyRound.addEntrant(first));
        assertTrue(emptyRound.hasEntrant(first));
    }

    @Test
    void oneEntrantRound_addEntrantAgain_returnsFalse() {
        assertFalse(oneEntrantRound.addEntrant(first));
        assertTrue(oneEntrantRound.hasEntrant(first));
    }

    @Test
    void emptyRound_addEntrant_isPending() {
        emptyRound.addEntrant(first);
        assertTrue(emptyRound.isEntrantPending(first));
    }

    @Test
    void singlePairFinishedRound_removeAdvancedThenAddBack_isAdvanced() {
        singlePairFinishedRound.removeEntrant(winner);
        singlePairFinishedRound.addEntrant(winner);
        assertTrue(singlePairFinishedRound.isEntrantAdvanced(winner));
        assertFalse(singlePairFinishedRound.isEntrantPending(winner));
    }

    // nextPairing()

    @Test
    void emptyRound_nextPairing_throwsNoEntrantsException() {
        assertThrows(NoEntrantsException.class, () -> emptyRound.nextPairing());
    }

    @Test
    void oneEntrantRound_nextPairing_throwsNoOpponentException() {
        assertThrows(NoOpponentException.class, () -> oneEntrantRound.nextPairing());
    }

    @Test
    void twoEntrantRound_nextPairing_noPendingEntrants() throws Exception {
        twoEntrantRound.nextPairing();
        assertTrue(twoEntrantRound.getPendingEntrants().isEmpty());
    }

    @Test
    void twoEntrantRound_nextPairing_bothEntrantsArePaired() {
        Assertions.assertDoesNotThrow(twoEntrantRound::nextPairing);
        assertFalse(twoEntrantRound.isEntrantPending(first));
        assertFalse(twoEntrantRound.isEntrantPending(second));
        assertTrue(twoEntrantRound.isEntrantPaired(first));
        assertTrue(twoEntrantRound.isEntrantPaired(second));
    }

    @Test
    void twoEntrantRound_nextPairing_returnedPairingContainsBothEntrants() throws Exception {
        Pairing<TestEntrant> pairing = twoEntrantRound.nextPairing();
        assertTrue(pairing.contains(first));
        assertTrue(pairing.contains(second));
    }

    @Test
    void singlePairFinishedRound_resetPairedEntrantsAndPairRandomAgain_entrantsArePaired() {
        singlePairFinishedRound.resetEntrant(winner);
        singlePairFinishedRound.resetEntrant(loser);
        Assertions.assertDoesNotThrow(singlePairFinishedRound::nextPairing);
        assertTrue(singlePairFinishedRound.isEntrantPaired(winner));
        assertTrue(singlePairFinishedRound.isEntrantPaired(loser));
    }

    @Test
    void multiEntrantRound_finishParallelPairingsOutOfOrder_identicalToInOrder() throws Exception {
        Pairing<TestEntrant> p1 = multiEntrantRound.nextPairing();
        Pairing<TestEntrant> p2 = multiEntrantRound.nextPairing();
        multiEntrantRound.declareWinner(p2.getFirst());
        multiEntrantRound.declareWinner(p1.getSecond());
        assertTrue(multiEntrantRound.isEntrantAdvanced(p1.getSecond()));
        assertTrue(multiEntrantRound.isEntrantAdvanced(p2.getFirst()));
        assertTrue(multiEntrantRound.isEntrantEliminated(p1.getFirst()));
        assertTrue(multiEntrantRound.isEntrantEliminated(p2.getSecond()));
    }

    // declareWinner()

    @Test
    void emptyRound_declareWinner_throwsNoSuchEntrantException() {
        assertThrows(NoSuchEntrantException.class, () -> emptyRound.declareWinner(winner));
    }

    @Test
    void oneEntrantRound_declareWinner_throwsMissingPairingException() {
        assertThrows(MissingPairingException.class, () -> oneEntrantRound.declareWinner(winner));
    }

    @Test
    void singlePairRound_declareFirstWinner_winnerAdvancedAndLoserEliminated() throws Exception {
        singlePairRound.declareWinner(winner);
        assertFalse(singlePairRound.isEntrantPaired(winner));
        assertFalse(singlePairRound.isEntrantPaired(loser));
        assertTrue(singlePairRound.isEntrantAdvanced(winner));
        assertTrue(singlePairRound.isEntrantEliminated(loser));
    }

    @Test
    void singlePairRound_declareWinner_returnsFinishedPairing() throws Exception {
        Pairing<TestEntrant> expectedPairing = twoEntrantRound.nextPairing();
        Pairing<TestEntrant> finishedPairing = twoEntrantRound.declareWinner(winner);
        assertSame(expectedPairing, finishedPairing);
    }

    @Test
    void singlePairRound_declareWinner_pairingFinished() throws Exception {
        Pairing<TestEntrant> pairing = singlePairRound.declareWinner(winner);
        assertTrue(singlePairRound.getFinishedPairings().contains(pairing));
    }

    @Test
    void multiPairRound_declareAllWinners_finishedPairingsAreOrderedChronologically() throws Exception {
        List<TestEntrant> winners = new ArrayList<>();
        while (multiEntrantRound.getPendingEntrants().size() >= 2) {
            Pairing<TestEntrant> pairing = multiEntrantRound.nextPairing();
            winners.add(pairing.getFirst());
        }

        Collections.shuffle(winners);
        for (TestEntrant winner : winners)
            multiEntrantRound.declareWinner(winner);

        List<Pairing<TestEntrant>> finishedPairings = new ArrayList<>(multiEntrantRound.getFinishedPairings());
        assertEquals(winners.size(), finishedPairings.size());
        for (int i = 0; i < winners.size(); ++i) {
            TestEntrant winner = winners.get(i);
            Pairing<TestEntrant> pairing = finishedPairings.get(i);
            assertTrue(pairing.getFirst() == winner || pairing.getSecond() == winner);
        }
    }

    // declareTie

    @Test
    void singlePairRound_declareTie_bothEntrantsEliminated() {
        singlePairRound.declareTie(singlePairRound.getActivePairings().iterator().next());
        assertTrue(singlePairRound.isEntrantEliminated(first));
        assertTrue(singlePairRound.isEntrantEliminated(second));
    }

    // replayPairing()

    @Test
    void singlePairRound_replayActivePairing_returnsFalse() throws Exception {
        Pairing<TestEntrant> pairing = twoEntrantRound.nextPairing();
        assertFalse(assertDoesNotThrow(() -> twoEntrantRound.replayPairing(pairing)));
    }

    @Test
    void twoEntrantRound_replayInvalidPairing_throwsNoSuchPairingException() {
        assertThrows(NoSuchPairingException.class, () ->
            twoEntrantRound.replayPairing(new Pairing<>(first, second)));
    }

    @Test
    void singlePairFinishedRound_replayPairing_bothEntrantsArePaired() {
        Pairing<TestEntrant> pairing = getFinishedPairing(singlePairFinishedRound);
        assertDoesNotThrow(() -> singlePairFinishedRound.replayPairing(pairing));
        assertTrue(singlePairFinishedRound.getActivePairings().contains(pairing));
        assertFalse(singlePairFinishedRound.hasEntrantResult(first));
        assertFalse(singlePairFinishedRound.hasEntrantResult(second));
    }

    @Test
    void singlePairFinishedRound_resetFirstEntrantAndReplayPairing_bothEntrantsArePaired() {
        Pairing<TestEntrant> pairing = getFinishedPairing(singlePairFinishedRound);
        singlePairFinishedRound.resetEntrant(first);
        assertDoesNotThrow(() -> singlePairFinishedRound.replayPairing(pairing));
        assertTrue(singlePairFinishedRound.getActivePairings().contains(pairing));
        assertFalse(singlePairFinishedRound.hasEntrantResult(first));
        assertFalse(singlePairFinishedRound.hasEntrantResult(second));
    }

    @Test
    void singlePairFinishedRound_resetFirstAndPairWithThirdThenReplayFirstPairing_throwsOrphanedPairingException() throws Exception {
        Pairing<TestEntrant> firstPairing = getFinishedPairing(singlePairFinishedRound);
        singlePairFinishedRound.addEntrant(third);
        singlePairFinishedRound.resetEntrant(first);

        // The first entrant is already paired with the third entrant.
        assertDoesNotThrow(() -> singlePairFinishedRound.nextPairing());
        assertThrows(OrphanedPairingException.class, () -> singlePairFinishedRound.replayPairing(firstPairing));

        // The first entrant is now in another finished round.
        assertDoesNotThrow(() -> singlePairFinishedRound.declareWinner(first));
        assertDoesNotThrow(() -> singlePairFinishedRound.replayPairing(firstPairing));

        // Resetting that entrant and then replaying should work.
        assertTrue(singlePairFinishedRound.isEntrantPaired(firstPairing.getFirst()));
        assertTrue(singlePairFinishedRound.isEntrantPaired(firstPairing.getSecond()));
    }

    @Test
    void singlePairFinishedRound_removeBothEntrantsAndReplayPairing_throwsMissingEntrantException() {
        Pairing<TestEntrant> pairing = getFinishedPairing(singlePairFinishedRound);
        singlePairFinishedRound.removeEntrant(first);
        singlePairFinishedRound.removeEntrant(second);
        assertThrows(MissingEntrantException.class, () -> singlePairFinishedRound.replayPairing(pairing));
    }

    @Test
    void singlePairFinishedRound_removeFirstEntrantAndReplayPairing_throwsMissingEntrantException() {
        Pairing<TestEntrant> pairing = getFinishedPairing(singlePairFinishedRound);
        singlePairFinishedRound.removeEntrant(first);
        assertThrows(MissingEntrantException.class, () -> singlePairFinishedRound.replayPairing(pairing));
    }

    // reset()

    @Test
    void singleEntrantRound_resetEntrant_isPending() {
        singlePairRound.resetEntrant(first);
        assertTrue(singlePairRound.isEntrantPending(first));
    }

    @Test
    void singleEntrantRound_resetInvalidEntrant_returnsFalse() {
        assertFalse(singlePairRound.resetEntrant(invalidEntrant));
        assertFalse(singlePairRound.isEntrantPending(invalidEntrant));
    }

    @Test
    void singlePairRound_resetFirst_isPending() {
        singlePairRound.resetEntrant(first);
        assertTrue(singlePairRound.isEntrantPending(first));
        assertFalse(singlePairRound.isEntrantPaired(first));
    }

    @Test
    void singlePairRound_resetFirst_secondIsPending() {
        singlePairRound.resetEntrant(second);
        assertTrue(singlePairRound.isEntrantPending(second));
        assertFalse(singlePairRound.isEntrantPaired(second));
    }

    @Test
    void singlePairFinishedRound_resetAdvanced_isPending() {
        singlePairFinishedRound.resetEntrant(winner);
        assertFalse(singlePairFinishedRound.isEntrantAdvanced(winner));
        assertTrue(singlePairFinishedRound.isEntrantPending(winner));
    }

    @Test
    void singlePairFinishedRound_resetAdvanced_eliminatedIsStillEliminated() {
        singlePairFinishedRound.resetEntrant(winner);
        assertTrue(singlePairFinishedRound.isEntrantEliminated(loser));
    }

    @Test
    void singlePairFinishedRound_resetFloatingAdvanced_isCompletelyRemoved() {
        singlePairFinishedRound.removeEntrant(winner);
        singlePairFinishedRound.resetEntrant(winner);
        assertFalse(singlePairFinishedRound.hasEntrant(winner));
        assertFalse(singlePairFinishedRound.isEntrantAdvanced(winner));
    }

    @Test
    void singlePairFinishedRound_resetAdvanced_keepsFinishedPairing() {
        singlePairFinishedRound.resetEntrant(winner);
        assertEquals(1, singlePairFinishedRound.getFinishedPairings().size());
    }

    @Test
    void singlePairFinishedRound_resetAdvancedAndEliminated_removesFinishedPairing() {
        singlePairFinishedRound.resetEntrant(winner);
        singlePairFinishedRound.resetEntrant(loser);
        assertTrue(singlePairFinishedRound.getFinishedPairings().isEmpty());
    }

    @Test
    void singlePairFinishedRound_removeWinnerAndResetLoser_finishedPairingStillExists() {
        singlePairFinishedRound.removeEntrant(winner);
        singlePairFinishedRound.resetEntrant(loser);
        assertEquals(1, singlePairFinishedRound.getFinishedPairings().size());
    }

    @Test
    void threeEntrantRound_resetLoserThenPairAgainstOther_hasTwoFinishedPairings() throws Exception {
        twoEntrantRound.addEntrant(third);
        DynamicElimination<TestEntrant> threeEntrantRound = twoEntrantRound;

        Pairing<TestEntrant> firstPairing = threeEntrantRound.nextPairing();
        TestEntrant firstWinner = firstPairing.getFirst();
        TestEntrant firstLoser = firstPairing.getSecond();

        threeEntrantRound.declareWinner(firstWinner);
        threeEntrantRound.resetEntrant(firstLoser);

        Pairing<TestEntrant> secondPairing = threeEntrantRound.nextPairing();
        TestEntrant secondLoser = secondPairing.getOther(firstLoser);

        threeEntrantRound.declareWinner(firstLoser);

        assertTrue(threeEntrantRound.isEntrantAdvanced(firstLoser));
        assertTrue(threeEntrantRound.isEntrantAdvanced(firstWinner));
        assertTrue(threeEntrantRound.isEntrantEliminated(secondLoser));
        assertEquals(2, threeEntrantRound.getFinishedPairings().size());

        // After resetting this player which participated in both pairings,
        // those pairings should still exist.
        threeEntrantRound.resetEntrant(firstLoser);
        assertEquals(2, threeEntrantRound.getFinishedPairings().size());
    }

    // remove()

    @Test
    void singlePairRound_removeFirst_isRemoved() {
        singlePairRound.removeEntrant(first);
        assertFalse(singlePairRound.hasEntrant(first));
        assertFalse(singlePairRound.isEntrantPending(first));
        assertFalse(singlePairRound.isEntrantPaired(first));
    }

    @Test
    void singlePairFinishedRound_removeWinnerAgain_returnsFalse() {
        singlePairFinishedRound.removeEntrant(winner);
        assertFalse(singlePairFinishedRound.removeEntrant(winner));
    }

    @Test
    void singlePairRound_removeFirst_secondIsPending() {
        singlePairRound.removeEntrant(first);
        assertTrue(singlePairRound.isEntrantPending(second));
        assertFalse(singlePairRound.isEntrantPaired(second));
    }

    @Test
    void singlePairFinishedRound_removeAdvanced_isRemoved() {
        assertTrue(singlePairFinishedRound.removeEntrant(winner));
        assertFalse(singlePairFinishedRound.hasEntrant(winner));
    }

    @Test
    void singlePairFinishedRound_removeAndResetAllEntrants_completelyEmptyRound() {
        singlePairFinishedRound.removeEntrant(first);
        singlePairFinishedRound.removeEntrant(second);
        singlePairFinishedRound.resetEntrant(first);
        singlePairFinishedRound.resetEntrant(second);
        assertFalse(singlePairFinishedRound.hasStateAbout(first));
        assertFalse(singlePairFinishedRound.hasStateAbout(second));
        assertTrue(singlePairFinishedRound.getPendingEntrants().isEmpty());
    }

    // isPairingOrphaned

    @Test
    void singlePairRound_finishFirstAndPairAnother_firstPairingIsOrphaned() throws Exception {
        Pairing<TestEntrant> firstPairing = singlePairRound.declareWinner(winner);
        singlePairRound.resetEntrant(loser);
        singlePairRound.addEntrant(third);
        Pairing<TestEntrant> secondPairing = singlePairRound.nextPairing();
        assertTrue(singlePairRound.isPairingOrphaned(firstPairing));
        assertFalse(singlePairRound.isPairingOrphaned(secondPairing));
        singlePairRound.declareWinner(loser);
        assertFalse(singlePairRound.isPairingOrphaned(firstPairing));
        assertFalse(singlePairRound.isPairingOrphaned(secondPairing));
    }

    // isFinished()

    @Test
    void singlePairFinishedRound_isFinished_returnsTrue() {
        assertTrue(singlePairFinishedRound.isFinished());
    }

    @Test
    void singlePairRound_isFinished_returnsFalse() {
        // The round is not finished since there is an active pairing.
        assertFalse(singlePairRound.isFinished());
        assertFalse(singlePairRound.getActivePairings().isEmpty());
    }

    @Test
    void singlePairFinishedRound_removeAdvanced_isStillFinished() {
        singlePairFinishedRound.removeEntrant(winner);
        assertTrue(singlePairFinishedRound.isFinished());
    }

    // Identities

    @Test
    void singlePairFinishedRound_resetThenRemoveAdvanced_identicalToRemoveThenResetAdvanced() {
        DynamicElimination<TestEntrant> otherRound = createSinglePairFinishedRound();

        singlePairFinishedRound.resetEntrant(winner);
        singlePairFinishedRound.removeEntrant(winner);
        otherRound.removeEntrant(winner);
        otherRound.resetEntrant(winner);

        assertEquals(singlePairFinishedRound.hasEntrant(winner), otherRound.hasEntrant(winner));
        assertEquals(singlePairFinishedRound.hasStateAbout(winner), otherRound.hasStateAbout(winner));
    }

    // Miscellaneous

    @Test
    void multiEntrantRound_nextPairing_isRandom() {
        assertSuppliesAll(entrants, () ->
            assertDoesNotThrow(() -> {
                DynamicElimination<TestEntrant> round = createMultiEntrantRound();
                Pairing<TestEntrant> pairing = round.nextPairing();
                return pairing.getFirst();
            })
        );
    }
}
