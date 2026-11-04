package com.halil.kilohedefim;

/** Yemeklerin ortalama porsiyon kalorileri (yaklaşık değerler). */
public class FoodDb {

    public static class Food {
        public final String cat, name, portion;
        public final int kcal;

        Food(String cat, String name, String portion, int kcal) {
            this.cat = cat;
            this.name = name;
            this.portion = portion;
            this.kcal = kcal;
        }
    }

    private static Food f(String c, String n, String p, int k) { return new Food(c, n, p, k); }

    public static final Food[] ALL = {
            // Kahvaltı
            f("Kahvaltı", "Haşlanmış yumurta", "1 adet", 75),
            f("Kahvaltı", "Omlet (2 yumurta)", "1 porsiyon", 190),
            f("Kahvaltı", "Menemen", "1 porsiyon", 220),
            f("Kahvaltı", "Sucuklu yumurta", "1 porsiyon", 350),
            f("Kahvaltı", "Beyaz peynir", "1 dilim (30 g)", 80),
            f("Kahvaltı", "Kaşar peyniri", "1 dilim (30 g)", 110),
            f("Kahvaltı", "Lor peyniri", "2 yemek kaşığı (40 g)", 40),
            f("Kahvaltı", "Siyah zeytin", "5 adet", 45),
            f("Kahvaltı", "Yeşil zeytin", "5 adet", 35),
            f("Kahvaltı", "Domates", "1 orta boy", 20),
            f("Kahvaltı", "Salatalık", "1 orta boy", 15),
            f("Kahvaltı", "Bal", "1 tatlı kaşığı", 30),
            f("Kahvaltı", "Reçel", "1 yemek kaşığı", 50),
            f("Kahvaltı", "Tereyağı", "1 tatlı kaşığı (5 g)", 36),
            f("Kahvaltı", "Fıstık ezmesi", "1 yemek kaşığı", 95),
            f("Kahvaltı", "Yulaf ezmesi (sütlü)", "1 kase", 250),
            f("Kahvaltı", "Simit", "1 adet", 300),
            f("Kahvaltı", "Poğaça", "1 adet", 250),
            f("Kahvaltı", "Su böreği", "1 porsiyon", 350),
            f("Kahvaltı", "Gözleme (peynirli)", "1 adet", 400),

            // Ekmek & tahıl
            f("Ekmek & Tahıl", "Beyaz ekmek", "1 dilim (25 g)", 70),
            f("Ekmek & Tahıl", "Tam buğday ekmeği", "1 dilim (25 g)", 60),
            f("Ekmek & Tahıl", "Lavaş", "1 adet", 170),
            f("Ekmek & Tahıl", "Pirinç pilavı", "1 porsiyon (150 g)", 250),
            f("Ekmek & Tahıl", "Bulgur pilavı", "1 porsiyon (150 g)", 200),
            f("Ekmek & Tahıl", "Makarna (sade)", "1 porsiyon (200 g pişmiş)", 300),
            f("Ekmek & Tahıl", "Haşlanmış patates", "1 orta boy", 130),
            f("Ekmek & Tahıl", "Patates kızartması", "1 porsiyon", 400),

            // Çorbalar
            f("Çorba", "Mercimek çorbası", "1 kase", 150),
            f("Çorba", "Ezogelin çorbası", "1 kase", 160),
            f("Çorba", "Tavuk suyu çorba", "1 kase", 120),
            f("Çorba", "Domates çorbası", "1 kase", 110),
            f("Çorba", "Yayla çorbası", "1 kase", 140),

            // Ana yemekler
            f("Ana Yemek", "Izgara tavuk göğsü", "150 g", 250),
            f("Ana Yemek", "Tavuk sote", "1 porsiyon", 300),
            f("Ana Yemek", "Izgara köfte", "4 adet (120 g)", 300),
            f("Ana Yemek", "Izgara balık", "1 porsiyon", 300),
            f("Ana Yemek", "Ton balığı (süzülmüş)", "1 kutu (80 g)", 100),
            f("Ana Yemek", "Kuru fasulye", "1 porsiyon", 280),
            f("Ana Yemek", "Nohut yemeği", "1 porsiyon", 280),
            f("Ana Yemek", "Zeytinyağlı taze fasulye", "1 porsiyon", 150),
            f("Ana Yemek", "Ispanak yemeği", "1 porsiyon", 140),
            f("Ana Yemek", "Etli türlü", "1 porsiyon", 250),
            f("Ana Yemek", "Karnıyarık", "1 adet", 350),
            f("Ana Yemek", "İmam bayıldı", "1 adet", 250),
            f("Ana Yemek", "Yaprak sarma", "5 adet", 180),
            f("Ana Yemek", "Mercimek köftesi", "4 adet", 200),
            f("Ana Yemek", "Mantı", "1 porsiyon", 500),
            f("Ana Yemek", "Lahmacun", "1 adet", 280),
            f("Ana Yemek", "Kıymalı pide", "1 adet", 700),
            f("Ana Yemek", "Döner dürüm", "1 adet", 600),
            f("Ana Yemek", "İskender", "1 porsiyon", 750),
            f("Ana Yemek", "Hamburger", "1 adet", 550),
            f("Ana Yemek", "Pizza", "1 dilim", 280),

            // Salata & yan
            f("Salata & Yan", "Mevsim salata (yağsız)", "1 kase", 40),
            f("Salata & Yan", "Çoban salata", "1 kase", 80),
            f("Salata & Yan", "Kısır", "1 porsiyon", 200),
            f("Salata & Yan", "Cacık", "1 kase", 90),
            f("Salata & Yan", "Zeytinyağı", "1 yemek kaşığı", 120),

            // Süt ürünleri
            f("Süt Ürünü", "Yoğurt (tam yağlı)", "1 kase (200 g)", 120),
            f("Süt Ürünü", "Yoğurt (light)", "1 kase (200 g)", 80),
            f("Süt Ürünü", "Ayran", "1 bardak", 75),
            f("Süt Ürünü", "Süt (yarım yağlı)", "1 su bardağı", 100),
            f("Süt Ürünü", "Kefir", "1 bardak", 110),

            // Meyveler
            f("Meyve", "Elma", "1 orta boy", 80),
            f("Meyve", "Muz", "1 orta boy", 105),
            f("Meyve", "Portakal", "1 orta boy", 65),
            f("Meyve", "Mandalina", "1 adet", 45),
            f("Meyve", "Armut", "1 orta boy", 100),
            f("Meyve", "Çilek", "1 kase", 50),
            f("Meyve", "Karpuz", "1 dilim (300 g)", 90),
            f("Meyve", "Kavun", "1 dilim", 60),
            f("Meyve", "Üzüm", "1 kase", 100),
            f("Meyve", "Kuru kayısı", "3 adet", 50),
            f("Meyve", "Hurma", "2 adet", 130),

            // Kuruyemiş
            f("Kuruyemiş", "Ceviz", "3 adet (15 g)", 100),
            f("Kuruyemiş", "Badem", "10 adet", 70),
            f("Kuruyemiş", "Fındık", "10 adet", 90),
            f("Kuruyemiş", "Leblebi", "1 avuç (30 g)", 110),
            f("Kuruyemiş", "Ay çekirdeği", "1 avuç (30 g)", 170),

            // Tatlı & atıştırmalık
            f("Tatlı & Atıştırmalık", "Baklava", "1 dilim", 165),
            f("Tatlı & Atıştırmalık", "Sütlaç", "1 kase", 250),
            f("Tatlı & Atıştırmalık", "Künefe", "1 porsiyon", 600),
            f("Tatlı & Atıştırmalık", "Sütlü çikolata", "4 kare (20 g)", 110),
            f("Tatlı & Atıştırmalık", "Bisküvi", "3 adet", 120),
            f("Tatlı & Atıştırmalık", "Cips", "küçük paket (30 g)", 160),
            f("Tatlı & Atıştırmalık", "Dondurma", "1 top", 100),
            f("Tatlı & Atıştırmalık", "Kek", "1 dilim", 250),

            // İçecekler
            f("İçecek", "Çay (şekersiz)", "1 bardak", 2),
            f("İçecek", "Çay (1 şekerli)", "1 bardak", 20),
            f("İçecek", "Türk kahvesi (sade)", "1 fincan", 10),
            f("İçecek", "Latte", "1 orta boy", 190),
            f("İçecek", "Kola", "1 kutu (330 ml)", 140),
            f("İçecek", "Meyve suyu", "1 su bardağı", 110),
            f("İçecek", "Şalgam", "1 bardak", 15),
    };

    /** Örnek menü şablonları: {öğün adı, içerik, kcal} */
    public static final String[][][] MENUS = {
            { // 1. gün
                    {"Kahvaltı", "2 haşlanmış yumurta, 1 dilim beyaz peynir, 5 zeytin, domates-salatalık, 1 dilim tam buğday ekmeği", "365"},
                    {"Öğle", "150 g ızgara tavuk, yarım porsiyon bulgur pilavı, yağsız mevsim salata, 1 ayran", "465"},
                    {"Ara öğün", "1 elma + 10 badem", "150"},
                    {"Akşam", "4 ızgara köfte, çoban salata, 1 kase cacık", "470"},
            },
            { // 2. gün
                    {"Kahvaltı", "1 kase sütlü yulaf ezmesi, 3 ceviz, birkaç dilim çilek", "370"},
                    {"Öğle", "1 kase mercimek çorbası, zeytinyağlı taze fasulye, 1 kase light yoğurt, 1 dilim tam buğday ekmeği", "440"},
                    {"Ara öğün", "1 kase light yoğurt + 1 kase çilek", "130"},
                    {"Akşam", "Izgara balık, yağsız mevsim salata, 1 haşlanmış patates", "470"},
            },
            { // 3. gün
                    {"Kahvaltı", "Menemen, 2 yemek kaşığı lor, salatalık, 1 dilim tam buğday ekmeği", "335"},
                    {"Öğle", "Ton balıklı salata (1 kutu ton, mevsim yeşillik, 1 yk zeytinyağı, 1 haşlanmış yumurta), 1 dilim tam buğday ekmeği", "395"},
                    {"Ara öğün", "1 portakal + 1 avuç leblebi", "175"},
                    {"Akşam", "1 porsiyon kuru fasulye, yarım porsiyon bulgur pilavı, 1 kase cacık", "470"},
            },
    };
}
