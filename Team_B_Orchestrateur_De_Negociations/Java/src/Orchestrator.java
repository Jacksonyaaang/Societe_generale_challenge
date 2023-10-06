import Communication.IPricerQueues;
import Communication.IQuoteQueues;
import utils.*;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class Orchestrator extends OrchestratorBase {
    protected final Double ExecutionTolerance = 0.000001;
    private ScheduledExecutorService _scheduler;
    // add code

    // Data structures for storing quotes and quote prices
    private final Map<Integer, Quote> quoteMap;
    private final Map<Integer, LinkedList<Double>> quotePriceMap;

    public Orchestrator(IQuoteQueues quoteQueues, IPricerQueues pricerQueues) {
        super(quoteQueues, pricerQueues);
        // TODO Initialize data structure to hold quotes, and to hold quote prices
        this.quoteMap = new HashMap<>();
        this.quotePriceMap = new HashMap<>();
    }

    @Override
    protected void OnQuoteRequest(Quote o) {
        // TODO Add quotes to the data structure
        this.quoteMap.put(o.getQuoteId(), o);
        NotifyQuoteReceived(o);
    }

    @Override
    protected void OnQuotePriceUpdate(QuotePrice o) {
        // TODO Update quote prices in quote prices data structure
        this.quotePriceMap.computeIfAbsent(o.Quote.getQuoteId(), k-> new LinkedList<Double>()).addFirst(o.Price);
        NotifyPriceUpdated(o);
    }

    @Override
    protected void OnExecutionRequest(QuoteExecutionPrice o) {
        
        var anyValidPrice = false;
        // TODO check if there is a quote price  from the last 3 quote prices
        //  that is equal to execution price within EXECUTION Tolerance
        LinkedList<Double> tempoList =  this.quotePriceMap.get(o.Quote.getQuoteId());

        for (int i=0;i<3; ++i){
            anyValidPrice = Math.abs(o.ExecutionPrice - tempoList.removeFirst()) <= ExecutionTolerance;
            if (anyValidPrice) break;
        }
        if (anyValidPrice)
        {
            o.Status = ExecutionStatus.Success;
            NotifyQuoteExecuted(o);
        }
        else
        {
            o.Status = ExecutionStatus.Fail;
            NotifyQuoteExecuted(o);
        }

        // TODO clean unwanted quotes and quote prices
        quoteMap.remove(o.Quote.getQuoteId());
        quotePriceMap.remove(o.Quote.getQuoteId());
    }

    @Override
    protected void OnQuoteStop(Quote o) {
        // TODO clean unwanted quotes and quote prices
        quoteMap.remove(o.getQuoteId());
        quotePriceMap.remove(o.getQuoteId());
        NotifyQuoteStopped(o);
    }

    @Override
    public void Start() {
        super.Start();
        
        _scheduler = Executors.newScheduledThreadPool(1);
        _scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                // TODO Schedule a pricing task
                // We should ask for a pricing for each quote that we have in memory
                // Communication with pricer can be done using the method RequestNewPrice
                for (Quote q : quoteMap.values()){
                    RequestNewPrice(q);
                }
            }
        }, 1, 2, TimeUnit.SECONDS);

    }

    @Override
    public void Stop() {
        super.Stop();
        _scheduler.shutdown();
    }
}
