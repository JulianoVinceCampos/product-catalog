package com.catalog.config;

import com.catalog.domain.model.Product;
import com.catalog.domain.model.ProductStatus;
import com.catalog.repository.jpa.ProductJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@Profile("seed")
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final ProductJpaRepository repo;

    @Override
    public void run(String... args) {
        if (repo.count() > 0) { log.info("[SEED] Already seeded, skipping."); return; }
        log.info("[SEED] Seeding sample products...");
        repo.saveAll(List.of(
            p("LAPTOP-MBPRO-001","MacBook Pro 14 M3 Pro","Apple M3 Pro chip, 18GB RAM, 512GB SSD",new BigDecimal("10999.00"),"Notebooks",ProductStatus.ACTIVE),
            p("LAPTOP-DELL-XPS15","Dell XPS 15 9530","Intel i9-13900H, 32GB DDR5, 1TB NVMe, OLED 3.5K",new BigDecimal("8499.00"),"Notebooks",ProductStatus.ACTIVE),
            p("PHONE-IPHONE15PRO","iPhone 15 Pro 256GB","A17 Pro, Titanium, 48MP, USB-C",new BigDecimal("7299.00"),"Smartphones",ProductStatus.ACTIVE),
            p("PHONE-SAMSUNG-S24U","Samsung Galaxy S24 Ultra","Snapdragon 8 Gen3, 12GB, 512GB, S Pen",new BigDecimal("6999.00"),"Smartphones",ProductStatus.ACTIVE),
            p("AUDIO-SONY-WH1000XM5","Sony WH-1000XM5","Noise cancellation, 30h battery, LDAC",new BigDecimal("1899.00"),"Audio",ProductStatus.ACTIVE),
            p("AUDIO-AIRPODS-PRO2","Apple AirPods Pro 2a Gen","ANC, Adaptive Audio, USB-C case",new BigDecimal("1599.00"),"Audio",ProductStatus.ACTIVE),
            p("MONITOR-LG-27UP850","LG 27\" 4K UHD IPS","3840x2160, USB-C 96W, HDR400, 60Hz",new BigDecimal("2799.00"),"Monitors",ProductStatus.ACTIVE),
            p("KEY-KEYCHRON-K2PRO","Keychron K2 Pro","Wireless Mechanical, RGB, QMK/VIA, hot-swap",new BigDecimal("599.00"),"Peripherals",ProductStatus.ACTIVE),
            p("MOUSE-LOGITECH-MX3S","Logitech MX Master 3S","8000 DPI, silent, USB-C, Bluetooth",new BigDecimal("449.00"),"Peripherals",ProductStatus.ACTIVE),
            p("TABLET-IPAD-PRO12","iPad Pro 12.9\" M2","M2 chip, Liquid Retina XDR, 256GB Wi-Fi",new BigDecimal("9299.00"),"Tablets",ProductStatus.INACTIVE)
        ));
        log.info("[SEED] Done.");
    }

    private Product p(String sku,String name,String desc,BigDecimal price,String cat,ProductStatus st){
        return Product.builder().sku(sku).name(name).description(desc).price(price).category(cat).status(st).build();
    }
}
