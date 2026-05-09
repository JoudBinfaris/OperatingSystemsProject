package proj;

import java.util.*;

public class SJFScheduler {

    private Queue<PCB> liveReadyQueue;
    private MemoryManager memoryManager;
    private List<PCB> processes;

    private Map<Integer, Integer> startTimeMap = new LinkedHashMap<>();
    private Map<Integer, Integer> terminationTimeMap = new LinkedHashMap<>();
    private Map<Integer, Integer> waitingTimeMap = new LinkedHashMap<>();
    private Map<Integer, Integer> turnaroundTimeMap = new LinkedHashMap<>();

    // ── Constructor ─────────────────────────────────────────────────────────

    public SJFScheduler(Queue<PCB> readyQueue, MemoryManager memoryManager) {
        this.liveReadyQueue = readyQueue;
        this.memoryManager = memoryManager;
        this.processes = new ArrayList<>(readyQueue);
    }

    // ── Main simulation ──────────────────────────────────────────────────────
    public void run() {

        System.out.println("\n==========================================");
        System.out.println("     Shortest Job First (SJF) Scheduler  ");
        System.out.println("==========================================\n");

        // Track added ID's
        Set<Integer> seen = new HashSet<>();
        for (PCB p : processes)
            seen.add(p.getProcessId());

        // sort working list
        List<PCB> workingList = new ArrayList<>(processes);
        workingList.sort(Comparator.comparingInt(PCB::getBurstTime));

        StringBuilder gantt = new StringBuilder();
        StringBuilder ganttTimes = new StringBuilder();
        int currentTime = 0;

        while (!workingList.isEmpty()) {

            PCB p = workingList.remove(0);
            p.setState(Pstate.RUNNING);

            // Calculate metrics
            int burstBefore = p.getBurstTime();
            int start = currentTime;
            int term = currentTime + p.getBurstTime();
            int waiting = currentTime - p.getArrivalTime();
            int turnaround = term - p.getArrivalTime();

            // Store in maps
            startTimeMap.put(p.getProcessId(), start);
            terminationTimeMap.put(p.getProcessId(), term);
            waitingTimeMap.put(p.getProcessId(), waiting);
            turnaroundTimeMap.put(p.getProcessId(), turnaround);

            p.setWaitingTime(waiting);
            p.setTurnaroundTime(turnaround);
            p.setState(Pstate.TERMINATED);

            // Build Gantt bar
            gantt.append(String.format("| P%d(%d->%d) ", p.getProcessId(), burstBefore, 0));
            ganttTimes.append(String.format("%-6d", start));

            currentTime = term;

            // ── Memory management ─────────
            if (memoryManager != null) {

                // Free this process's memory
                memoryManager.freeMemory(p);

                // Try to admit any previously rejected processes
                memoryManager.retryRejected();

                // Check if any new processes were admitted to the live ready queue
                for (PCB newP : liveReadyQueue) {
                    if (!seen.contains(newP.getProcessId())) {
                        seen.add(newP.getProcessId());
                        processes.add(newP);
                        workingList.add(newP);
                    }
                }

                // Re-sort working list
                workingList.sort(Comparator.comparingInt(PCB::getBurstTime));
            }
        }

        gantt.append("|");
        ganttTimes.append(currentTime);

        printGantt(gantt, ganttTimes);
        printTable();
        printAverages();
    }

    // ── Output helpers ───────────────────────────────────────────────────────

    private void printGantt(StringBuilder gantt, StringBuilder ganttTimes) {
        System.out.println("Gantt Chart:");
        System.out.println(gantt);
        System.out.println(ganttTimes);
        System.out.println();
    }

    private void printTable() {
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

        for (PCB p : processes) {
            int id = p.getProcessId();
            System.out.printf(
                    "%-12d %-12d %-12d %-18d %-14d %-16d%n",
                    id,
                    p.getBurstTime(),
                    startTimeMap.get(id),
                    terminationTimeMap.get(id),
                    waitingTimeMap.get(id),
                    turnaroundTimeMap.get(id));
        }

        System.out.println(divider);
        System.out.println();
    }

    private void printAverages() {
        double avgWaiting = 0;
        double avgTurnaround = 0;

        for (PCB p : processes) {
            avgWaiting += waitingTimeMap.get(p.getProcessId());
            avgTurnaround += turnaroundTimeMap.get(p.getProcessId());
        }

        avgWaiting /= processes.size();
        avgTurnaround /= processes.size();

        System.out.printf("Average Waiting Time    : %.2f ms%n", avgWaiting);
        System.out.printf("Average Turnaround Time : %.2f ms%n", avgTurnaround);
        System.out.println();
    }

}