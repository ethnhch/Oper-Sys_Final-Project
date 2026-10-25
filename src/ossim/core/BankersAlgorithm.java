package ossim.core;

import ossim.util.InputException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Deadlock avoidance: Need computation, safety algorithm and resource-request algorithm. */
public final class BankersAlgorithm {

    public record Result(int[][] need, boolean safe, List<Integer> safeSequence, List<String> trace) { }

    public record RequestResult(boolean granted, String reason, Result stateAfter) { }

    private BankersAlgorithm() { }

    public static Result analyze(int[][] allocation, int[][] max, int[] available) {
        validate(allocation, max, available);
        int n = allocation.length;
        int m = available.length;
        int[][] need = new int[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                need[i][j] = max[i][j] - allocation[i][j];
            }
        }

        int[] work = available.clone();
        boolean[] finished = new boolean[n];
        List<Integer> sequence = new ArrayList<>();
        List<String> trace = new ArrayList<>();
        trace.add("Work = Available = " + Arrays.toString(work));

        boolean progress = true;
        while (sequence.size() < n && progress) {
            progress = false;
            for (int i = 0; i < n; i++) {
                if (finished[i] || !fits(need[i], work)) {
                    continue;
                }
                trace.add("P" + i + ": Need " + Arrays.toString(need[i]) + " <= Work " + Arrays.toString(work)
                        + "  ->  runs to completion and releases " + Arrays.toString(allocation[i]));
                for (int j = 0; j < m; j++) {
                    work[j] += allocation[i][j];
                }
                trace.add("      Work = " + Arrays.toString(work));
                finished[i] = true;
                sequence.add(i);
                progress = true;
            }
        }
        boolean safe = sequence.size() == n;
        if (!safe) {
            for (int i = 0; i < n; i++) {
                if (!finished[i]) {
                    trace.add("P" + i + ": Need " + Arrays.toString(need[i]) + " > Work " + Arrays.toString(work) + "  ->  blocked");
                }
            }
        }
        return new Result(need, safe, safe ? sequence : List.of(), trace);
    }

    /** Resource-request algorithm: grants the request only if the resulting state is safe. */
    public static RequestResult request(int[][] allocation, int[][] max, int[] available, int process, int[] request) {
        Result current = analyze(allocation, max, available);
        int m = available.length;
        if (process < 0 || process >= allocation.length) {
            throw new InputException("Process P" + process + " does not exist.");
        }
        if (request.length != m) {
            throw new InputException("Request must have exactly " + m + " values.");
        }
        if (!fits(request, current.need()[process])) {
            return new RequestResult(false, "Denied: P" + process + " exceeds its maximum claim (Request > Need).", current);
        }
        if (!fits(request, available)) {
            return new RequestResult(false, "Must wait: Request > Available.", current);
        }
        int[][] tentativeAlloc = copy(allocation);
        int[] tentativeAvail = available.clone();
        for (int j = 0; j < m; j++) {
            tentativeAlloc[process][j] += request[j];
            tentativeAvail[j] -= request[j];
        }
        Result after = analyze(tentativeAlloc, max, tentativeAvail);
        return after.safe()
                ? new RequestResult(true, "Granted: the resulting state is safe.", after)
                : new RequestResult(false, "Denied: granting the request leads to an unsafe state.", after);
    }

    private static boolean fits(int[] need, int[] work) {
        for (int j = 0; j < need.length; j++) {
            if (need[j] > work[j]) {
                return false;
            }
        }
        return true;
    }

    private static int[][] copy(int[][] matrix) {
        int[][] out = new int[matrix.length][];
        for (int i = 0; i < matrix.length; i++) {
            out[i] = matrix[i].clone();
        }
        return out;
    }

    private static void validate(int[][] allocation, int[][] max, int[] available) {
        if (allocation.length == 0 || available.length == 0) {
            throw new InputException("At least one process and one resource type are required.");
        }
        for (int i = 0; i < allocation.length; i++) {
            for (int j = 0; j < available.length; j++) {
                if (allocation[i][j] < 0 || max[i][j] < 0) {
                    throw new InputException("Negative values are not allowed (P" + i + ", R" + j + ").");
                }
                if (allocation[i][j] > max[i][j]) {
                    throw new InputException("Inconsistent input: Allocation exceeds Max for P" + i + ", R" + j + ".");
                }
            }
        }
        for (int j = 0; j < available.length; j++) {
            if (available[j] < 0) {
                throw new InputException("Available R" + j + " cannot be negative.");
            }
        }
    }
}
