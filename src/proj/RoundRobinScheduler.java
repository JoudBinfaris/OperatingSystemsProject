package proj;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * RoundRobinScheduler - Round Robin (Preemptive) Scheduling Algorithm
 *
 * Rules:
 * - Each process gets a fixed time quantum (q = 5 ms) of CPU time.
 * - If a process does not finish within its quantum, it is preempted
 * and moved to the end of the ready queue.
 * - All processes are assumed to arrive at time 0.
 * - Processes are served in arrival order (order in the input list).
 *
 * Usage:
 * RoundRobinScheduler scheduler = new RoundRobinScheduler(readyQueue);
 * scheduler.run();
 */
public class RoundRobinScheduler {

    private static final int TIME_QUANTUM = 5; // q = 5 ms (project requirement)

    private List<PCB> processes; // local copy of the ready queue (for table output)
    private Queue<PCB> readyQueue; // working queue we cycle through

    // ── Constructor ──────────────────────────────────────────────────────────
    public RoundRobinScheduler(List<PCB> readyQueue) {
        // Work on copies so we don't disturb the original queue
        this.processes = new ArrayList<>(readyQueue);
        this.readyQueue = new LinkedList<>(readyQueue);
    }

    // ── Main simulation ──────────────────────────────────────────────────────
    public void run() {

        System.out.println("\n╔══════════════════════════════════════════╗");
        System.out.println("║   Round Robin Scheduler  (q = " + TIME_QUANTUM + " ms)      ║");
        System.out.println("╚══════════════════════════════════════════╝\n");

        // Step 1 – Initialize tracking arrays (indexed by position in 'processes')
        int n = processes.size();

        int[] remainingBurst = new int[n];
        int[] startTime = new int[n];
        int[] terminationTime = new int[n];
        int[] waitingTime = new int[n];
        int[] turnaroundTime = new int[n];
        boolean[] hasStarted = new boolean[n];

        // Initialize each process's remaining burst to its full burst time
        for (int i = 0; i < n; i++) {
            remainingBurst[i] = processes.get(i).getBurstTime();
        }

        // Gantt chart: each entry is "| P<id> " followed by the start time below
        StringBuilder gantt = new StringBuilder();
        StringBuilder ganttTimes = new StringBuilder();

        // Step 2 – Simulate execution by cycling the queue
        int currentTime = 0;

        while (!readyQueue.isEmpty()) {
            PCB p = readyQueue.poll(); // take from front of queue
            int idx = indexOf(p); // find this process's slot in our arrays

            p.setState(Pstate.RUNNING);

            // Record start time only the FIRST time this process gets the CPU
            if (!hasStarted[idx]) {
                startTime[idx] = currentTime;
                hasStarted[idx] = true;
            }

            // Run for either a full quantum or whatever burst remains, whichever is smaller
            int runTime = Math.min(TIME_QUANTUM, remainingBurst[idx]);

            // Build Gantt bar
            gantt.append(String.format("| P%-2d ", p.getProcessId()));
            ganttTimes.append(String.format("%-6d", currentTime));

            // Advance the clock and reduce remaining burst
            currentTime += runTime;
            remainingBurst[idx] -= runTime;

            if (remainingBurst[idx] == 0) {
                // Process finished
                terminationTime[idx] = currentTime;
                turnaroundTime[idx] = terminationTime[idx] - p.getArrivalTime();
                waitingTime[idx] = turnaroundTime[idx] - p.getBurstTime();

                // Store back into PCB
                p.setWaitingTime(waitingTime[idx]);
                p.setTurnaroundTime(turnaroundTime[idx]);
                p.setState(Pstate.TERMINATED);
            } else {
                // Still has burst left → put it back at the END of the queue
                p.setState(Pstate.READY);
                readyQueue.offer(p);
            }
        }

        // Close Gantt bar and append final time
        gantt.append("|");
        ganttTimes.append(currentTime);

        // Step 3 – Print Gantt chart
        printGantt(gantt, ganttTimes);

        // Step 4 – Print results table
        printTable(n, startTime, terminationTime, waitingTime, turnaroundTime);

        // Step 5 – Print averages
        printAverages(n, waitingTime, turnaroundTime);
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    /** Find the index of a process in the local 'processes' list by ID. */
    private int indexOf(PCB p) {
        for (int i = 0; i < processes.size(); i++) {
            if (processes.get(i).getProcessId() == p.getProcessId()) {
                return i;
            }
        }
        return -1; // should never happen
    }

    // ── Output helpers ───────────────────────────────────────────────────────

    private void printGantt(StringBuilder gantt, StringBuilder ganttTimes) {
        System.out.println("Gantt Chart:");
        System.out.println(gantt);
        System.out.println(ganttTimes);
        System.out.println();
    }

    private void printTable(int n,
            int[] startTime,
            int[] terminationTime,
            int[] waitingTime,
            int[] turnaroundTime) {

        String header = String.format(
                "%-12s %-12s %-12s %-18s %-14s %-16s",
                "Process ID", "Burst Time", "Start Time",
                "Termination Time", "Waiting Time", "Turnaround Time");

        String divider = "";
        for (int i = 0; i < header.length(); i++)
            divider += "-";

        System.out.println("Results Table:");
        System.out.println(divider);
        System.out.println(header);
        System.out.println(divider);

        for (int i = 0; i < n; i++) {
            PCB p = processes.get(i);
            System.out.printf(
                    "%-12d %-12d %-12d %-18d %-14d %-16d%n",
                    p.getProcessId(),
                    p.getBurstTime(),
                    startTime[i],
                    terminationTime[i],
                    waitingTime[i],
                    turnaroundTime[i]);
        }

        System.out.println(divider);
        System.out.println();
    }

    private void printAverages(int n, int[] waitingTime, int[] turnaroundTime) {
        double avgWaiting = 0;
        double avgTurnaround = 0;

        for (int i = 0; i < n; i++) {
            avgWaiting += waitingTime[i];
            avgTurnaround += turnaroundTime[i];
        }

        avgWaiting /= n;
        avgTurnaround /= n;

        System.out.printf("Average Waiting Time    : %.2f ms%n", avgWaiting);
        System.out.printf("Average Turnaround Time : %.2f ms%n", avgTurnaround);
        System.out.println();
    }

    // ── Standalone test (hardcoded processes) ────────────────────────────────
    public static void main(String[] args) {

        // Same processes as SJFScheduler, for easy comparison:
        // ID : Burst : Priority ; Memory
        // 1 : 25 : 4 ; 500
        // 2 : 13 : 3 ; 700
        // 3 : 20 : 3 ; 100
        // 4 : 18 : 2 ; 200

        List<PCB> readyQueue = new ArrayList<>();
        readyQueue.add(new PCB(1, 25, 4, 500));
        readyQueue.add(new PCB(2, 13, 3, 700));
        readyQueue.add(new PCB(3, 20, 3, 100));
        readyQueue.add(new PCB(4, 18, 2, 200));

        RoundRobinScheduler rr = new RoundRobinScheduler(readyQueue);
        rr.run();
    }
}
