package com.childrenofmu.entity.ithk;

import net.minecraft.util.RandomSource;

/** Ithk names. Add more freely; one is picked at random when an Ithk first appears. */
public final class IthkNames {
    private static final String[] NAMES = {
            // Originals
            "Ouk", "Tau'De", "Chk Chk", "Yral",
            // Short, guttural
            "Ith'Va", "Oon", "Kr'Ael", "Sseth", "Ylu", "Zhi'Tk", "Ommu", "Tchk", "Vael", "Iu'Ra",
            "Hraak", "Qel", "Nn'Ith", "Uuthe", "Ghu", "Ekk", "Thaal", "Yth", "Vo'Ok", "Rrin",
            "Uul", "Kesh", "Ooth", "Tikk", "Aa'Ul", "Shrr", "Xu", "Ilk", "Mmon", "Zaa'Th",
            // Doubled clicks, like Chk Chk
            "Tk Tk", "Kik Kik", "Tsk Tsk", "Kch Kch", "Ook Ook", "Thk Thk", "Pik Pik", "Ssk Ssk",
            // Apostrophe pairs, like Tau'De
            "Yau'Te", "Ruu'Ka", "Sho'Ith", "Ka'Ru", "Ve'Oth", "Ith'Ka", "Mu'Ra", "Tho'Ul",
            "Ae'Thk", "Yo'Rr", "Ze'Ith", "Uu'Mo", "Kre'Tu", "Va'Shi", "Ol'Ik", "Ny'Aa",
            "Tse'Ou", "Ri'Ath", "Ho'Kk", "Ek'Ut", "Lu'Vae", "Sha'Ou", "Te'Yr", "Ou'Ka",
            // Longer, older-sounding
            "Yrathe", "Ouketh", "Thaelu", "Ithrak", "Vaurn", "Ommelith", "Kthaal", "Sevrou",
            "Yurathk", "Naamu", "Ulthe", "Raavik", "Ohmth", "Tchaell", "Yvrae", "Ithmuu",
            "Kaeloth", "Zuurn", "Ouvaith", "Sraak", "Thrynn", "Eloum", "Vuthe", "Akkoth"
    };

    public static String random(RandomSource random) {
        return NAMES[random.nextInt(NAMES.length)];
    }

    private IthkNames() {}
}
