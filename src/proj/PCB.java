package proj;

public class PCB {
	private int pId;
	private Pstate state;
	private int burstTime;
	private int priority;
	private int memoryRequired; // in MB
	private int waitingTime;
	private int turnaroundTime;
	private int arrivalTime; // 0 

	public PCB(int processId, int burstTime, int priority, int memoryRequired) {
		this.pId = processId;
		this.burstTime = burstTime;
		this.priority = priority;
		this.memoryRequired = memoryRequired;
		this.state = Pstate.NEW;
		this.arrivalTime = 0;
		this.waitingTime = 0;
		this.turnaroundTime = 0;
	}


	public int getProcessId() {
		return pId;
	}

	public Pstate getState() {
		return state;
	}

	public void setState(Pstate state) {
		this.state = state;
	}

	public int getBurstTime() {
		return burstTime;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	} // Needed for aging

	public int getMemoryRequired() {
		return memoryRequired;
	}

	public int getWaitingTime() {
		return waitingTime;
	}

	public void setWaitingTime(int waitingTime) {
		this.waitingTime = waitingTime;
	}

	public int getTurnaroundTime() {
		return turnaroundTime;
	}

	public void setTurnaroundTime(int turnaroundTime) {
		this.turnaroundTime = turnaroundTime;
	}

	public int getArrivalTime() {
		return arrivalTime;
	}

	@Override
	public String toString() {
		return "PCB ID=" + pId + ", Burst=" + burstTime + "ms, Priority=" + priority + ", Memory="
				+ memoryRequired + "MB\n";
	}
}
