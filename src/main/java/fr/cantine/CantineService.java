package fr.cantine;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

@Service
public class CantineService {
    public BigDecimal getBalance() {
        return new BigDecimal("30.00");
    }

    public BigDecimal getMaxDebt() {
        return new BigDecimal("-20.00");
    }
}
