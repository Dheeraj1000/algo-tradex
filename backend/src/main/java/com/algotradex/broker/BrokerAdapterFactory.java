package com.algotradex.broker;

import com.algotradex.model.enums.BrokerType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class BrokerAdapterFactory {

    private final Map<BrokerType, BrokerAdapter> adapters;

    public BrokerAdapterFactory(List<BrokerAdapter> adapterList) {
        this.adapters = adapterList.stream()
                .collect(Collectors.toMap(BrokerAdapter::getBrokerType, Function.identity()));
    }

    public BrokerAdapter getAdapter(BrokerType type) {
        BrokerAdapter adapter = adapters.get(type);
        if (adapter == null) {
            throw new IllegalArgumentException("Unsupported broker type: " + type);
        }
        return adapter;
    }
}
