package proj;

import java.util.*;


public class PriorityScheduler {

    private static final int AGING_INTERVAL = 4;
    private static final int MIN_PRIORITY   = 1;

    private Queue<PCB>    liveReadyQueue; 
    private MemoryManager memoryManager;  
    private List<PCB>     processes;      

    private Map<Integer, Integer> startTimeMap       = new LinkedHashMap<>();
    private Map<Integer, Integer> terminationTimeMap = new LinkedHashMap<>();
    private Map<Integer, Integer> waitingTimeMap     = new LinkedHashMap<>();
    private Map<Integer, Integer> turnaroundTimeMap  = new LinkedHashMap<>();
    private Map<Integer, Boolean> starvedMap         = new LinkedHashMap<>();
    private Map<Integer, Integer> waitedSoFarMap     = new LinkedHashMap<>();
    private Set<Integer>          seen               = new HashSet<>();

    // ── Constructors ─────────────────────────────────────────────────────────

    public PriorityScheduler(Queue<PCB> readyQueue, MemoryManager memoryManager) {
        this.liveReadyQueue = readyQueue;
        this.memoryManager  = memoryManager;
        this.processes      = new ArrayList<>(readyQueue);
        initMaps();
    }

    /** Initialize tracking maps for all currently known processes */
    private void initMaps() {
        for (PCB p : processes) {
            seen.add(p.getProcessId());
            starvedMap.put(p.getProcessId(), false);
            waitedSoFarMap.put(p.getProcessId(), 0);
        }
    }

    // ── Main simulation ──────────────────────────────────────────────────────
    public void run() {

        System.out.println("\n==========================================");
        System.out.println("   Priority Scheduler (Non-Preemptive)   ");
        System.out.println("==========================================\n");

        // ── Working list ───────────────────
        List<PCB> waitingQueue = new ArrayList<>(processes);

        StringBuilder gantt      = new StringBuilder();
        StringBuilder ganttTimes = new StringBuilder();

        int currentTime    = 0;
        int lastAgingTime  = 0;

        while (!waitingQueue.isEmpty()) {

            // ── Pull in any newly admitted processes ──────────────────────
            if (memoryManager != null) {
                for (PCB newP : liveReadyQueue) {
                    if (!seen.contains(newP.getProcessId())) {
                        seen.add(newP.getProcessId());
                        processes.add(newP);
                        waitingQueue.add(newP);
                        starvedMap.put(newP.getProcessId(), false);
                        waitedSoFarMap.put(newP.getProcessId(), 0);
                    }
                }
            }

            // ── Apply aging every 4 ms ────────────────────────────────────
            while (currentTime - lastAgingTime >= AGING_INTERVAL) {
                lastAgingTime += AGING_INTERVAL;
                for (PCB p : waitingQueue) {
                    int newPriority = Math.max(MIN_PRIORITY, p.getPriority() - 1);
                    p.setPriority(newPriority);
                }
            }

            // ── Check for starvation ──────────────────────────────────────
            int starvationThreshold = waitingQueue.size() * 5;
            for (PCB p : waitingQueue) {
                if (waitedSoFarMap.get(p.getProcessId()) > starvationThreshold) {
                    starvedMap.put(p.getProcessId(), true);
                }
            }

            // ── Pick highest priority process ─────────────────
            waitingQueue.sort(Comparator.comparingInt(PCB::getPriority));
            PCB p  = waitingQueue.remove(0);
            int id = p.getProcessId();

            p.setState(Pstate.RUNNING);

            int burstBefore = p.getBurstTime();
            int start       = currentTime;
            int term        = currentTime + p.getBurstTime();
            int waiting     = currentTime - p.getArrivalTime();
            int turnaround  = term - p.getArrivalTime();

            startTimeMap.put(id, start);
            terminationTimeMap.put(id, term);
            waitingTimeMap.put(id, waiting);
            turnaroundTimeMap.put(id, turnaround);

            p.setWaitingTime(waiting);
            p.setTurnaroundTime(turnaround);
            p.setState(Pstate.TERMINATED);

            gantt.append(String.format("| P%d(%d->%d) ", id, burstBefore, 0));
            ganttTimes.append(String.format("%-12d", start));

            // Update waitedSoFar for all remaining processes
            int runDuration = p.getBurstTime();
            for (PCB waiting2 : waitingQueue) {
                int wId = waiting2.getProcessId();
                waitedSoFarMap.put(wId, waitedSoFarMap.get(wId) + runDuration);
            }

            currentTime = term;

            // ── Memory management ─────────
            if (memoryManager != null) {
                memoryManager.freeMemory(p);
                try { Thread.sleep(10); } catch (InterruptedException e) {}

                
            }
        }

        gantt.append("|");
        ganttTimes.append(currentTime);

        printGantt(gantt, ganttTimes);
        printTable();
        printAverages();
        printStarvationReport();
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
        for (int i = 0; i < header.length(); i++) divider += "-";

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
        double avgWaiting    = 0;
        double avgTurnaround = 0;

        for (PCB p : processes) {
            avgWaiting    += waitingTimeMap.get(p.getProcessId());
            avgTurnaround += turnaroundTimeMap.get(p.getProcessId());
        }

        avgWaiting    /= processes.size();
        avgTurnaround /= processes.size();

        System.out.printf("Average Waiting Time    : %.2f ms%n", avgWaiting);
        System.out.printf("Average Turnaround Time : %.2f ms%n", avgTurnaround);
        System.out.println();
    }

    private void printStarvationReport() {
        System.out.println("Starvation Report:");
        boolean anyStarved = false;

        for (PCB p : processes) {
            if (starvedMap.get(p.getProcessId())) {
                System.out.println("  Process P" + p.getProcessId()
                    + " suffered from starvation. Aging was applied.");
                anyStarved = true;
            }
        }

        if (!anyStarved) {
            System.out.println("  No processes suffered from starvation.");
        }

        System.out.println();
    }

}