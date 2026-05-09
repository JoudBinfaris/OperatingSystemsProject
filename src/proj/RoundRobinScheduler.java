package proj;

import java.util.*;


public class RoundRobinScheduler {

    private static final int TIME_QUANTUM = 5;

    private Queue<PCB>    liveReadyQueue; 
    private MemoryManager memoryManager;  
    private List<PCB>     processes;      
    private Queue<PCB>    workingQueue;   

    
    private Map<Integer, Integer> remainingBurstMap  = new LinkedHashMap<>();
    private Map<Integer, Integer> startTimeMap       = new LinkedHashMap<>();
    private Map<Integer, Integer> terminationTimeMap = new LinkedHashMap<>();
    private Map<Integer, Integer> waitingTimeMap     = new LinkedHashMap<>();
    private Map<Integer, Integer> turnaroundTimeMap  = new LinkedHashMap<>();
    private Set<Integer>          hasStarted         = new HashSet<>();
    private Set<Integer>          seen               = new HashSet<>();

    // ── Constructors ─────────────────────────────────────────────────────────

    public RoundRobinScheduler(Queue<PCB> readyQueue, MemoryManager memoryManager) {
        this.liveReadyQueue = readyQueue;
        this.memoryManager  = memoryManager;
        this.processes      = new ArrayList<>(readyQueue);
        this.workingQueue   = new LinkedList<>(readyQueue);
        initRemainingBurst();
    }

    /* Initialize remaining burst for all currently known processes */
    private void initRemainingBurst() {
        for (PCB p : processes) {
            remainingBurstMap.put(p.getProcessId(), p.getBurstTime());
            seen.add(p.getProcessId());
        }
    }

    // ── Main simulation ──────────────────────────────────────────────────────
    public void run() {

        System.out.println("\n==========================================");
        System.out.println("   Round Robin Scheduler  (q = " + TIME_QUANTUM + " ms)   ");
        System.out.println("==========================================\n");

        StringBuilder gantt      = new StringBuilder();
        StringBuilder ganttTimes = new StringBuilder();
        int currentTime = 0;

        while (!workingQueue.isEmpty()) {

            PCB p  = workingQueue.poll();
            int id = p.getProcessId();

            p.setState(Pstate.RUNNING);

            // Record start time only on first CPU visit
            if (!hasStarted.contains(id)) {
                startTimeMap.put(id, currentTime);
                hasStarted.add(id);
            }

            // Run for either a full quantum or whatever burst remains
              int remaining   = remainingBurstMap.get(id);
            int burstBefore = remaining;
            int runTime     = Math.min(TIME_QUANTUM, remaining);
 
            ganttTimes.append(String.format("%-12d", currentTime));
 
            currentTime += runTime;
            remaining   -= runTime;
            remainingBurstMap.put(id, remaining);
 
            int burstAfter = remaining;
            gantt.append(String.format("| P%d(%d->%d) ", p.getProcessId(), burstBefore, burstAfter));


            if (remaining == 0) {

                // Process fully finished
                int term       = currentTime;
                int turnaround = term - p.getArrivalTime();
                int waiting    = turnaround - p.getBurstTime();

                terminationTimeMap.put(id, term);
                turnaroundTimeMap.put(id, turnaround);
                waitingTimeMap.put(id, waiting);

                p.setWaitingTime(waiting);
                p.setTurnaroundTime(turnaround);
                p.setState(Pstate.TERMINATED);

                // ── Memory management ─────
                if (memoryManager != null) {

                    // Free this process's memory
                    memoryManager.freeMemory(p);

                    // Try to admit any previously rejected processes
                    memoryManager.retryRejected();

                    // Check if new processes were admitted to the live ready queue
                    for (PCB newP : liveReadyQueue) {
                        if (!seen.contains(newP.getProcessId())) {
                            seen.add(newP.getProcessId());
                            processes.add(newP);
                            workingQueue.offer(newP); 
                            remainingBurstMap.put(newP.getProcessId(), newP.getBurstTime());
                        }
                    }
                }

            } else {
                // Still has burst remaining — goes back to end of queue
                p.setState(Pstate.READY);
                workingQueue.offer(p);
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

}