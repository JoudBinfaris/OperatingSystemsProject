package proj;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * SJFScheduler - Shortest Job First (Non-Preemptive) Scheduling Algorithm
 *
 * Rules:
 * - Processes are sorted by CPU burst time (shortest first).
 * - Ties in burst time are broken by arrival order (order in the input list).
 * - All processes are assumed to arrive at time 0.
 *
 * Usage:
 * SJFScheduler scheduler = new SJFScheduler(readyQueue);
 * scheduler.run();
 */
public class SJFScheduler {

    private List<PCB> processes; // local copy of the ready queue

    // ── Constructor ──────────────────────────────────────────────────────────
    public SJFScheduler(List<PCB> readyQueue) {
        // Work on a copy so we don't disturb the original queue
        this.processes = new ArrayList<>(readyQueue);
    }

    // ── Main simulation ──────────────────────────────────────────────────────
    public void run() {

        System.out.println("\n==========================================");
System.out.println("     Shortest Job First (SJF) Scheduler  ");
System.out.println("==========================================\n");

        // Step 1 – Sort by burst time; use original list index as tiebreaker
        // (stable sort preserves insertion order for equal elements)
        processes.sort(Comparator.comparingInt(PCB::getBurstTime));

        // Step 2 – Simulate execution and collect results
        int currentTime = 0;
        int n = processes.size();

        int[] startTime = new int[n];
        int[] terminationTime = new int[n];
        int[] waitingTime = new int[n];
        int[] turnaroundTime = new int[n];

        // Gantt chart: each entry is "| P<id> (start-end) "
        StringBuilder gantt = new StringBuilder();
        StringBuilder ganttTimes = new StringBuilder();

        for (int i = 0; i < n; i++) {
            PCB p = processes.get(i);

            p.setState(Pstate.RUNNING);

            startTime[i] = currentTime;
            terminationTime[i] = currentTime + p.getBurstTime();
            waitingTime[i] = currentTime - p.getArrivalTime(); // arrival = 0
            turnaroundTime[i] = terminationTime[i] - p.getArrivalTime();

            // Store back into PCB
            p.setWaitingTime(waitingTime[i]);
            p.setTurnaroundTime(turnaroundTime[i]);
            p.setState(Pstate.TERMINATED);

            // Build Gantt bar
            gantt.append(String.format("| P%-2d ", p.getProcessId()));
            ganttTimes.append(String.format("%-6d", startTime[i]));

            currentTime = terminationTime[i];
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

        // Mirrors the sample from job.txt:
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

        SJFScheduler sjf = new SJFScheduler(readyQueue);
        sjf.run();
    }
}
