package de.j13g.manko.core;

import de.j13g.manko.core.base.EliminationRound;
import de.j13g.manko.core.exceptions.*;
import de.j13g.manko.util.ShuffledSet;
import de.j13g.manko.util.UniformPairLinkedBiSet;
import de.j13g.manko.util.UniformPairUniqueBiSet;
import de.j13g.manko.util.exceptions.EmptySetException;
import de.j13g.manko.util.exceptions.NoSuchElementException;

import java.io.Serializable;
import java.util.*;

public class DynamicRound<E extends Serializable> implements EliminationRound<E>, Serializable {

    private final HashSet<E> entrants = new HashSet<>();
    private final ShuffledSet<E> pendingEntrants = new ShuffledSet<>();

    private final Results<E> results = new Results<>();
    private final Results<E> floatingResults = new Results<>();

    private final UniformPairUniqueBiSet<E, Pairing<E>> activePairings = new UniformPairUniqueBiSet<>();
    private final UniformPairLinkedBiSet<E, Pairing<E>> finishedPairings = new UniformPairLinkedBiSet<>();

    public DynamicRound() {}

    public DynamicRound(Set<E> entrants) {
        entrants.forEach(this::add);
    }

    @Override
    public boolean add(E entrant) {
        if (entrants.contains(entrant))
            return false;

        if (floatingResults.contains(entrant)) {
            floatingResults.moveTo(results, entrant);
            return true;
        }

        entrants.add(entrant);
        pendingEntrants.add(entrant);
        return true;
    }

    @Override
    public Pairing<E> createPairing(E entrant1, E entrant2) throws NoSuchEntrantException, EntrantNotPendingException {
        if (!contains(entrant1) || !contains(entrant2))
            throw new NoSuchEntrantException();
        if (!isPending(entrant1) || !isPending(entrant2))
            throw new EntrantNotPendingException();

        pendingEntrants.remove(entrant1);
        pendingEntrants.remove(entrant2);
        return registerPairing(entrant1, entrant2);
    }

    @Override
    public Pairing<E> nextPairing() throws NoEntrantsException, NoOpponentException {
        if (pendingEntrants.size() == 0) throw new NoEntrantsException();
        if (pendingEntrants.size() == 1) throw new NoOpponentException();

        try {
            E entrant1 = pendingEntrants.removeRandom();
            E entrant2 = pendingEntrants.removeRandom();
            return registerPairing(entrant1, entrant2);
        }
        catch (EmptySetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Pairing<E> declareWinner(E entrant) throws NoSuchEntrantException, MissingPairingException {
        if (!contains(entrant))
            throw new NoSuchEntrantException();

        Pairing<E> pairing = activePairings.findByElement(entrant);
        if (pairing == null)
            throw new MissingPairingException();

        results.advance(entrant);
        results.eliminate(getOtherUnsafe(pairing, entrant));

        finishedPairings.add(pairing);
        activePairings.remove(pairing);

        // Check that entrants don't end up where they shouldn't.
        assert entrants.size() == 2 * activePairings.size() +
            finishedPairings.getPairElementSet().size() + pendingEntrants.size();
        assert entrants.size() == 2 * activePairings.size() +
            results.getAdvanced().size() + results.getEliminated().size() + pendingEntrants.size();

        return pairing;
    }

    @Override
    public boolean replayPairing(Pairing<E> pairing)
            throws NoSuchPairingException, MissingEntrantException, EntrantNotPendingException {
        if (activePairings.contains(pairing))
            return false;
        if (!finishedPairings.contains(pairing))
            throw new NoSuchPairingException();

        E first = pairing.getFirst();
        E second = pairing.getSecond();

        // One of the entrants could be removed,
        // since finished pairings are only removed if both entrants are gone.
        if (!contains(first) || !contains(second))
            throw new MissingEntrantException();

        boolean firstHasResult = hasResult(first);
        boolean secondHasResult = hasResult(second);
        assert firstHasResult || secondHasResult;

        if (!firstHasResult || !secondHasResult) {
            E withoutResult = firstHasResult ? second : first;
            if (!isPending(withoutResult))
                throw new EntrantNotPendingException();
        }

        results.reset(first);
        results.reset(second);
        finishedPairings.remove(pairing);

        registerPairing(first, second);

        return true; // TODO
    }

    @Override
    public boolean reset(E entrant) {
        if (!hasStateAbout(entrant) || isPending(entrant))
            return false;

        if (isPaired(entrant)) {
            Pairing<E> pairing = activePairings.removeByElement(entrant);
            resetOtherUnsafe(pairing, entrant);
            pendingEntrants.add(entrant);
        }
        else if (isAdvanced(entrant) || isEliminated(entrant)) {
            results.reset(entrant);
            pendingEntrants.add(entrant);
        }
        else if (floatingResults.isAdvanced(entrant) || floatingResults.isEliminated(entrant)) {
            assert !entrants.contains(entrant);
            floatingResults.reset(entrant);
        }

        Set<Pairing<E>> entrantPairingSet = finishedPairings.findByElement(entrant);

        // Create a copy because removing elements from finishedPairings
        // in the loop below will modify the original set.
        List<Pairing<E>> entrantPairings = new ArrayList<>(entrantPairingSet);

        // Remove all pairings that this entrant is part of
        // and where the other entrant does not have any results
        // i.e. the other entrant was reset before too.
        for (Pairing<E> pairing : entrantPairings) {
            E other = getOtherUnsafe(pairing, entrant);
            if (!hasResult(other) && !floatingResults.contains(other))
                finishedPairings.remove(pairing);
        }

        return true;
    }

    @Override
    public boolean remove(E entrant) {
        if (isPending(entrant)) {
            pendingEntrants.remove(entrant);
        }
        else if (isPaired(entrant)) {
            Pairing<E> pairing = activePairings.findByElement(entrant);
            resetOtherUnsafe(pairing, entrant);
            activePairings.remove(pairing);
        }
        else if (isAdvanced(entrant) || isEliminated(entrant)) {
            boolean wasMoved = results.moveTo(floatingResults, entrant);
            // NOTE: Separate variable required so that
            // the assert does not have side effects.
            assert wasMoved;
        }
        else if (floatingResults.contains(entrant)) {
            return false; // Already removed.
        }

        return entrants.remove(entrant);
    }

    @Override
    public boolean contains(E entrant) {
        return entrants.contains(entrant);
    }

    /**
     * Checks if the entrant participates in this round
     * or has won or lost a pairing before and was not reset since then.
     * @param entrant The entrant.
     * @return If any state is associated to this entrant.
     */
    public boolean hasStateAbout(E entrant) {
        return contains(entrant) || floatingResults.contains(entrant);
    }

    @Override
    public boolean hasResult(E entrant) {
        return results.contains(entrant);
    }

    @Override
    public boolean isPending(E entrant) {
        return pendingEntrants.contains(entrant);
    }

    @Override
    public boolean isPaired(E entrant) {
        return activePairings.findByElement(entrant) != null;
    }

    @Override
    public boolean isAdvanced(E entrant) {
        return results.isAdvanced(entrant);
    }

    @Override
    public boolean isEliminated(E entrant) {
        return results.isEliminated(entrant);
    }

    public boolean hasActivePairings() {
        return !getActivePairings().isEmpty();
    }

    @Override
    public boolean isFinished() {
        // Assert either not finished or proper entrant distribution.
        assert !(pendingEntrants.isEmpty() && activePairings.isEmpty())
            || entrants.size() == results.getAdvanced().size() + results.getEliminated().size();

        return pendingEntrants.isEmpty() && activePairings.isEmpty();
    }

    @Override
    public Set<E> getPendingEntrants() {
        return pendingEntrants.elements();
    }

    @Override
    public Set<Pairing<E>> getActivePairings() {
        return activePairings.elements();
    }

    @Override
    public Set<Pairing<E>> getFinishedPairings() {
        return finishedPairings.elements();
    }

    @Override
    public Set<E> getAdvancedEntrants() {
        return results.getAdvanced();
    }

    @Override
    public Set<E> getEliminatedEntrants() {
        return results.getEliminated();
    }

    /**
     * Creates a new pairing with two participants.
     * Does not check if the participants are part of the round or are pending.
     * @param entrant1 The first entrant.
     * @param entrant2 The second entrant.
     * @return The created pairing containing both entrants.
     */
    private Pairing<E> registerPairing(E entrant1, E entrant2) {

        Pairing<E> pairing = new Pairing<>(entrant1, entrant2);

        assert !activePairings.contains(pairing);
        assert !finishedPairings.contains(pairing);

        activePairings.add(pairing);
        return pairing;
    }

    private E getOtherUnsafe(Pairing<E> pairing, E entrant) {
        try {
            return pairing.getOther(entrant);
        } catch (NoSuchElementException e) {
            throw new RuntimeException(e);
        }
    }

    private void resetOtherUnsafe(Pairing<E> pairing, E entrant) {
        E other = getOtherUnsafe(pairing, entrant);
        pendingEntrants.add(other);
    }
}