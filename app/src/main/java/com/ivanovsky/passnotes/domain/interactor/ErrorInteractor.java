package com.ivanovsky.passnotes.domain.interactor;

import android.content.Context;

import com.ivanovsky.passnotes.BuildConfig;
import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.data.entity.OperationError;

public class ErrorInteractor {

    private final Context context;

    public ErrorInteractor(Context context) {
        this.context = context;
    }

    public String processAndGetMessage(OperationError error) {
        return getMessage(error);
    }

    public ErrorProcessingResult process(OperationError error) {
        return new ErrorProcessingResult(getMessage(error), getResolution(error));
    }

    private String getMessage(OperationError error) {
        String message;

        if (BuildConfig.DEBUG) {
            message = formatDebugMessageFromOperationError(error);
        } else {
            message = getDefaultMessageForOperationErrorType(error.getType());
        }

        return message;
    }

    private ErrorResolution getResolution(OperationError error) {
        switch (error.getType()) {
            case NETWORK_IO_ERROR:
                return ErrorResolution.RETRY;
            default:
                return ErrorResolution.NOTHING;
        }
    }

    private String getDefaultMessageForOperationErrorType(OperationError.Type type) {
        //TODO: implement other type handling
        switch (type) {
            case NETWORK_IO_ERROR:
                return context.getString(R.string.network_error_was_occurred_check_internet_connection);
            default:
                return context.getString(R.string.error_was_occurred);
        }
    }

    private String formatDebugMessageFromOperationError(OperationError error) {
        StringBuilder sb = new StringBuilder();

        sb.append(error.getType());

        if (error.getMessage() != null) {
            sb.append(": ").append(error.getMessage());
        }

        if (error.getThrowable() != null) {
            sb.append(": ").append(error.getThrowable().toString());
        }

        return sb.toString();
    }
}
