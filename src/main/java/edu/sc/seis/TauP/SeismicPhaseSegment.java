package edu.sc.seis.TauP;

public class SeismicPhaseSegment {
	TauModel tMod;
	int startBranch;
    int endBranch;
    boolean isPWave;
    int endAction;
    boolean isDownGoing;
    String legName;
    
	public SeismicPhaseSegment(TauModel tMod,
			                   int startBranch,
                               int endBranch,
                               boolean isPWave,
                               int endAction,
                               boolean isDownGoing,
                               String legName) {
		this.tMod = tMod;
		this.startBranch = startBranch;
		this.endBranch = endBranch;
		this.isPWave = isPWave;
		this.endAction = endAction;
		this.isDownGoing = isDownGoing;
		this.legName = legName;
	}
	
	public static String endActionToString(int endAction) {
		String action;
		switch(endAction) {
    	case SeismicPhase.TURN:
    		action = "turn";
    		break;
    	case SeismicPhase.REFLECT_UNDERSIDE:
    		action = "reflect underside";
    		break;
    	case SeismicPhase.REFLECT_TOPSIDE:
    		action = "reflect topside";
    		break;
    	case SeismicPhase.TRANSUP:
    		action = "transmit up";
    		break;
    	case SeismicPhase.TRANSDOWN:
    		action = "transmit down";
    		break;
    	case SeismicPhase.DIFFRACT:
    		action = "diffract";
    		break;
    	default: 
    		// should never happen
    		action = "unknown";
    	}
		return action;
	}
	
	public String describeBranchRange(int startBranch, int endBranch) {
		String out = "";
		if (startBranch < tMod.getMohoBranch() && endBranch < tMod.getMohoBranch()) {
			out = "crust";
		} else if (startBranch < tMod.getCmbBranch() && endBranch < tMod.getCmbBranch()) {
			if (startBranch < tMod.getMohoBranch() && endBranch > tMod.getMohoBranch()) {
				out = "crust/mantle";
			} else if (startBranch > tMod.getMohoBranch() && endBranch < tMod.getMohoBranch()) {
				out = "crust/mantle";
			} else {
				out = "mantle";
			}
		} else if (startBranch <= tMod.getIocbBranch() && endBranch <= tMod.getIocbBranch()) {
			out = "outer core";
		} else {
			out = "inner core";
		}
		return out;
	}
	
	public String toString() {
		String desc = "";
    	String upDown = isDownGoing ? "down" : "up  ";
    	String action = endActionToString(endAction);
    	String isPString = isPWave ? "P" : "S";
    	String branchRange = startBranch == endBranch ? " layer "+startBranch : " layer "+startBranch+" to "+endBranch;
    	
    	if ( ! legName.contentEquals("END")) {
    		desc += legName +" going "+upDown
    				+ " as a "+ isPString 
    				+ " in the "+describeBranchRange(startBranch, endBranch)+","
    	    	    + branchRange+","
    				+ " then " +action;
    	} else {
    		desc += "END";
    	}
		return desc;
	}
}
