package core;

import java.util.List;

public class CommonDataBus {

    // simple policy: if multiple ready, pick the one with smallest sequence number (passed in externally)

    public void broadcastOne(CdbMessage msg,
                             List<ReservationStation> rsList,
                             List<LoadBuffer> loadBuffers,
                             List<StoreBuffer> storeBuffers,
                             RegisterFile regFile) {
        if (msg == null) return;

        // 1) write into register file (only if Qi matches)
        if (msg.destRegIndex() >= 0) {
            Register dest = regFile.get(msg.destRegIndex());
            if (msg.tag().equals(dest.getQi())) {
                dest.setValue(msg.value());
                dest.setQi(Tag.NONE);
            }
        }

        // 2) forward to RS
        for (ReservationStation rs : rsList) {
            rs.onCdbBroadcast(msg.tag(), msg.value());
        }

        // 3) forward to load/store buffers

        // loads don't usually need this, but in case of store forwarding
        
        //stores
        for (StoreBuffer sb : storeBuffers) {
            sb.onCdbBroadcast(msg.tag(), msg.value());
        }
    }
}
