package com.batch.process.listener;

import java.util.EventObject;

public class JobEvent extends EventObject {
    private static final long serialVersionUID = 1L;

    public JobEvent(BatchJobInfo source) {
        super(source);
    }

    @Override
    public BatchJobInfo getSource() {
        return (BatchJobInfo) super.getSource();
    }
}
