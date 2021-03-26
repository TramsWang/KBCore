package sinc.util;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import sinc.common.Constant;
import sinc.common.Predicate;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RKBTest {
    static final String TABLE_LINKED = "linked";
    static final int ARITY_LINKED = 2;
    static final String TABLE_CONNECTED = "connected";
    static final int ARITY_CONNECTED = 2;
    static final String TABLE_MALE = "male";
    static final int ARITY_MALE = 1;
    static final String TABLE_FEMALE = "female";
    static final int ARITY_FEMALE = 1;
    static final String TABLE_FATHER = "father";
    static final int ARITY_FATHER = 2;
    static final String TABLE_MOTHER = "mother";
    static final int ARITY_MOTHER = 2;
    static final String TABLE_ROAD = "road";
    static final int ARITY_ROAD = 5;
    static final String TABLE_ROAD_TYPE = "roadType";
    static final int ARITY_ROAD_TYPE = 3;
    static final int CONSTANT_ID = RKB.CONSTANT_ID;

    static final RKB kb;

    static {
        RKB tmp;
        try {
            tmp = new RKB(null);
        } catch (Exception e) {
            tmp = null;
        }
        kb = tmp;
    }

    @BeforeAll
    static void createKB() throws SQLException {
        /* 创建有向图KB */
        kb.defineFunctor(TABLE_LINKED, ARITY_LINKED);
        kb.defineFunctor(TABLE_CONNECTED, ARITY_CONNECTED);

        Predicate linked1 = new Predicate(TABLE_LINKED, ARITY_LINKED);
        linked1.args[0] = new Constant(CONSTANT_ID, "a");
        linked1.args[1] = new Constant(CONSTANT_ID, "b");
        Predicate linked2 = new Predicate(TABLE_LINKED, ARITY_LINKED);
        linked2.args[0] = new Constant(CONSTANT_ID, "b");
        linked2.args[1] = new Constant(CONSTANT_ID, "c");
        Predicate linked3 = new Predicate(TABLE_LINKED, ARITY_LINKED);
        linked3.args[0] = new Constant(CONSTANT_ID, "a");
        linked3.args[1] = new Constant(CONSTANT_ID, "d");
        Predicate linked4 = new Predicate(TABLE_LINKED, ARITY_LINKED);
        linked4.args[0] = new Constant(CONSTANT_ID, "e");
        linked4.args[1] = new Constant(CONSTANT_ID, "a");

        Predicate connected1 = new Predicate(TABLE_CONNECTED, ARITY_CONNECTED);
        connected1.args[0] = new Constant(CONSTANT_ID, "a");
        connected1.args[1] = new Constant(CONSTANT_ID, "b");
        Predicate connected2 = new Predicate(TABLE_CONNECTED, ARITY_CONNECTED);
        connected2.args[0] = new Constant(CONSTANT_ID, "b");
        connected2.args[1] = new Constant(CONSTANT_ID, "c");
        Predicate connected3 = new Predicate(TABLE_CONNECTED, ARITY_CONNECTED);
        connected3.args[0] = new Constant(CONSTANT_ID, "a");
        connected3.args[1] = new Constant(CONSTANT_ID, "d");
        Predicate connected4 = new Predicate(TABLE_CONNECTED, ARITY_CONNECTED);
        connected4.args[0] = new Constant(CONSTANT_ID, "e");
        connected4.args[1] = new Constant(CONSTANT_ID, "a");
        Predicate connected5 = new Predicate(TABLE_CONNECTED, ARITY_CONNECTED);
        connected5.args[0] = new Constant(CONSTANT_ID, "a");
        connected5.args[1] = new Constant(CONSTANT_ID, "c");
        Predicate connected6 = new Predicate(TABLE_CONNECTED, ARITY_CONNECTED);
        connected6.args[0] = new Constant(CONSTANT_ID, "e");
        connected6.args[1] = new Constant(CONSTANT_ID, "c");
        Predicate connected7 = new Predicate(TABLE_CONNECTED, ARITY_CONNECTED);
        connected7.args[0] = new Constant(CONSTANT_ID, "e");
        connected7.args[1] = new Constant(CONSTANT_ID, "b");
        Predicate connected8 = new Predicate(TABLE_CONNECTED, ARITY_CONNECTED);
        connected8.args[0] = new Constant(CONSTANT_ID, "e");
        connected8.args[1] = new Constant(CONSTANT_ID, "d");

        kb.addPredicate(linked1);
        kb.addPredicate(linked2);
        kb.addPredicate(linked3);
        kb.addPredicate(linked4);
        kb.addPredicate(connected1);
        kb.addPredicate(connected2);
        kb.addPredicate(connected3);
        kb.addPredicate(connected4);
        kb.addPredicate(connected5);
        kb.addPredicate(connected6);
        kb.addPredicate(connected7);
        kb.addPredicate(connected8);

        /* 创建家庭KB */
        kb.defineFunctor(TABLE_MALE, ARITY_MALE);
        kb.defineFunctor(TABLE_FEMALE, ARITY_FEMALE);
        kb.defineFunctor(TABLE_MOTHER, ARITY_MOTHER);
        kb.defineFunctor(TABLE_FATHER, ARITY_FATHER);

        Predicate male1 = new Predicate(TABLE_MALE, ARITY_MALE);
        male1.args[0] = new Constant(CONSTANT_ID, "tom");
        Predicate male2 = new Predicate(TABLE_MALE, ARITY_MALE);
        male2.args[0] = new Constant(CONSTANT_ID, "jerry");
        Predicate male3 = new Predicate(TABLE_MALE, ARITY_MALE);
        male3.args[0] = new Constant(CONSTANT_ID, "bob");

        Predicate female1 = new Predicate(TABLE_FEMALE, ARITY_FEMALE);
        female1.args[0] = new Constant(CONSTANT_ID, "amie");
        Predicate female2 = new Predicate(TABLE_FEMALE, ARITY_FEMALE);
        female2.args[0] = new Constant(CONSTANT_ID, "laura");

        Predicate father1 = new Predicate(TABLE_FATHER, ARITY_FATHER);
        father1.args[0] = new Constant(CONSTANT_ID, "tom");
        father1.args[1] = new Constant(CONSTANT_ID, "jerry");
        Predicate father2 = new Predicate(TABLE_FATHER, ARITY_FATHER);
        father2.args[0] = new Constant(CONSTANT_ID, "jerry");
        father2.args[1] = new Constant(CONSTANT_ID, "laura");

        Predicate mother1 = new Predicate(TABLE_MOTHER, ARITY_MOTHER);
        mother1.args[0] = new Constant(CONSTANT_ID, "amie");
        mother1.args[1] = new Constant(CONSTANT_ID, "laura");
        Predicate mother2 = new Predicate(TABLE_MOTHER, ARITY_MOTHER);
        mother2.args[0] = new Constant(CONSTANT_ID, "amie");
        mother2.args[1] = new Constant(CONSTANT_ID, "bob");

        kb.addPredicate(male1);
        kb.addPredicate(male2);
        kb.addPredicate(male3);
        kb.addPredicate(female1);
        kb.addPredicate(female2);
        kb.addPredicate(father1);
        kb.addPredicate(father2);
        kb.addPredicate(mother1);
        kb.addPredicate(mother2);

        /* 创建交通图KB */
        kb.defineFunctor(TABLE_ROAD, ARITY_ROAD);
        kb.defineFunctor(TABLE_ROAD_TYPE, ARITY_ROAD_TYPE);

        Predicate road1 = new Predicate(TABLE_ROAD, ARITY_ROAD);
        road1.args[0] = new Constant(CONSTANT_ID, "济青高速");
        road1.args[1] = new Constant(CONSTANT_ID, "济南");
        road1.args[2] = new Constant(CONSTANT_ID, "东");
        road1.args[3] = new Constant(CONSTANT_ID, "青岛");
        road1.args[4] = new Constant(CONSTANT_ID, "西");
        Predicate road2 = new Predicate(TABLE_ROAD, ARITY_ROAD);
        road2.args[0] = new Constant(CONSTANT_ID, "胶济铁路");
        road2.args[1] = new Constant(CONSTANT_ID, "济南");
        road2.args[2] = new Constant(CONSTANT_ID, "南");
        road2.args[3] = new Constant(CONSTANT_ID, "胶州湾");
        road2.args[4] = new Constant(CONSTANT_ID, "南");
        Predicate road3 = new Predicate(TABLE_ROAD, ARITY_ROAD);
        road3.args[0] = new Constant(CONSTANT_ID, "上海内环");
        road3.args[1] = new Constant(CONSTANT_ID, "上海");
        road3.args[2] = new Constant(CONSTANT_ID, "北");
        road3.args[3] = new Constant(CONSTANT_ID, "上海");
        road3.args[4] = new Constant(CONSTANT_ID, "东");
        Predicate road4 = new Predicate(TABLE_ROAD, ARITY_ROAD);
        road4.args[0] = new Constant(CONSTANT_ID, "上海外环");
        road4.args[1] = new Constant(CONSTANT_ID, "上海");
        road4.args[2] = new Constant(CONSTANT_ID, "南");
        road4.args[3] = new Constant(CONSTANT_ID, "上海");
        road4.args[4] = new Constant(CONSTANT_ID, "南");

        Predicate road_type1 = new Predicate(TABLE_ROAD_TYPE, ARITY_ROAD_TYPE);
        road_type1.args[0] = new Constant(CONSTANT_ID, "济青高速");
        road_type1.args[1] = new Constant(CONSTANT_ID, "公路");
        road_type1.args[2] = new Constant(CONSTANT_ID, "直");
        Predicate road_type2 = new Predicate(TABLE_ROAD_TYPE, ARITY_ROAD_TYPE);
        road_type2.args[0] = new Constant(CONSTANT_ID, "胶济铁路");
        road_type2.args[1] = new Constant(CONSTANT_ID, "铁路");
        road_type2.args[2] = new Constant(CONSTANT_ID, "弯");
        Predicate road_type3 = new Predicate(TABLE_ROAD_TYPE, ARITY_ROAD_TYPE);
        road_type3.args[0] = new Constant(CONSTANT_ID, "上海内环");
        road_type3.args[1] = new Constant(CONSTANT_ID, "公路");
        road_type3.args[2] = new Constant(CONSTANT_ID, "环");
        Predicate road_type4 = new Predicate(TABLE_ROAD_TYPE, ARITY_ROAD_TYPE);
        road_type4.args[0] = new Constant(CONSTANT_ID, "上海外环");
        road_type4.args[1] = new Constant(CONSTANT_ID, "公路");
        road_type4.args[2] = new Constant(CONSTANT_ID, "环");

        kb.addPredicate(road1);
        kb.addPredicate(road2);
        kb.addPredicate(road3);
        kb.addPredicate(road4);
        kb.addPredicate(road_type1);
        kb.addPredicate(road_type2);
        kb.addPredicate(road_type3);
        kb.addPredicate(road_type4);
    }

    @Test
    void testKBCreation() throws SQLException {
        /* 有向图KB */
        System.out.println("有向图KB");
        Predicate linked1 = new Predicate(TABLE_LINKED, ARITY_LINKED);
        linked1.args[0] = new Constant(CONSTANT_ID, "a");
        linked1.args[1] = new Constant(CONSTANT_ID, "b");
        Predicate linked2 = new Predicate(TABLE_LINKED, ARITY_LINKED);
        linked2.args[0] = new Constant(CONSTANT_ID, "b");
        linked2.args[1] = new Constant(CONSTANT_ID, "c");
        Predicate linked3 = new Predicate(TABLE_LINKED, ARITY_LINKED);
        linked3.args[0] = new Constant(CONSTANT_ID, "a");
        linked3.args[1] = new Constant(CONSTANT_ID, "d");
        Predicate linked4 = new Predicate(TABLE_LINKED, ARITY_LINKED);
        linked4.args[0] = new Constant(CONSTANT_ID, "e");
        linked4.args[1] = new Constant(CONSTANT_ID, "a");
        Set<Predicate> linked_set = new HashSet<>();
        linked_set.add(linked1);
        System.out.println(linked1);
        linked_set.add(linked2);
        System.out.println(linked2);
        linked_set.add(linked3);
        System.out.println(linked3);
        linked_set.add(linked4);
        System.out.println(linked4);
        assertEquals(linked_set, kb.listPredicate(TABLE_LINKED, ARITY_LINKED));

        Predicate connected1 = new Predicate(TABLE_CONNECTED, ARITY_CONNECTED);
        connected1.args[0] = new Constant(CONSTANT_ID, "a");
        connected1.args[1] = new Constant(CONSTANT_ID, "b");
        Predicate connected2 = new Predicate(TABLE_CONNECTED, ARITY_CONNECTED);
        connected2.args[0] = new Constant(CONSTANT_ID, "b");
        connected2.args[1] = new Constant(CONSTANT_ID, "c");
        Predicate connected3 = new Predicate(TABLE_CONNECTED, ARITY_CONNECTED);
        connected3.args[0] = new Constant(CONSTANT_ID, "a");
        connected3.args[1] = new Constant(CONSTANT_ID, "d");
        Predicate connected4 = new Predicate(TABLE_CONNECTED, ARITY_CONNECTED);
        connected4.args[0] = new Constant(CONSTANT_ID, "e");
        connected4.args[1] = new Constant(CONSTANT_ID, "a");
        Predicate connected5 = new Predicate(TABLE_CONNECTED, ARITY_CONNECTED);
        connected5.args[0] = new Constant(CONSTANT_ID, "a");
        connected5.args[1] = new Constant(CONSTANT_ID, "c");
        Predicate connected6 = new Predicate(TABLE_CONNECTED, ARITY_CONNECTED);
        connected6.args[0] = new Constant(CONSTANT_ID, "e");
        connected6.args[1] = new Constant(CONSTANT_ID, "c");
        Predicate connected7 = new Predicate(TABLE_CONNECTED, ARITY_CONNECTED);
        connected7.args[0] = new Constant(CONSTANT_ID, "e");
        connected7.args[1] = new Constant(CONSTANT_ID, "b");
        Predicate connected8 = new Predicate(TABLE_CONNECTED, ARITY_CONNECTED);
        connected8.args[0] = new Constant(CONSTANT_ID, "e");
        connected8.args[1] = new Constant(CONSTANT_ID, "d");
        Set<Predicate> connected_set = new HashSet<>();
        connected_set.add(connected1);
        System.out.println(connected1);
        connected_set.add(connected2);
        System.out.println(connected2);
        connected_set.add(connected3);
        System.out.println(connected3);
        connected_set.add(connected4);
        System.out.println(connected4);
        connected_set.add(connected5);
        System.out.println(connected5);
        connected_set.add(connected6);
        System.out.println(connected6);
        connected_set.add(connected7);
        System.out.println(connected7);
        connected_set.add(connected8);
        System.out.println(connected8);
        assertEquals(connected_set, kb.listPredicate(TABLE_CONNECTED, ARITY_CONNECTED));
        System.out.println();

        /* 家庭KB */
        System.out.println("家庭KB");
        Predicate male1 = new Predicate(TABLE_MALE, ARITY_MALE);
        male1.args[0] = new Constant(CONSTANT_ID, "tom");
        Predicate male2 = new Predicate(TABLE_MALE, ARITY_MALE);
        male2.args[0] = new Constant(CONSTANT_ID, "jerry");
        Predicate male3 = new Predicate(TABLE_MALE, ARITY_MALE);
        male3.args[0] = new Constant(CONSTANT_ID, "bob");
        Set<Predicate> male_set = new HashSet<>();
        male_set.add(male1);
        System.out.println(male1);
        male_set.add(male2);
        System.out.println(male2);
        male_set.add(male3);
        System.out.println(male3);
        assertEquals(male_set, kb.listPredicate(TABLE_MALE, ARITY_MALE));

        Predicate female1 = new Predicate(TABLE_FEMALE, ARITY_FEMALE);
        female1.args[0] = new Constant(CONSTANT_ID, "amie");
        Predicate female2 = new Predicate(TABLE_FEMALE, ARITY_FEMALE);
        female2.args[0] = new Constant(CONSTANT_ID, "laura");
        Set<Predicate> female_set = new HashSet<>();
        female_set.add(female1);
        System.out.println(female1);
        female_set.add(female2);
        System.out.println(female2);
        assertEquals(female_set, kb.listPredicate(TABLE_FEMALE, ARITY_FEMALE));

        Predicate father1 = new Predicate(TABLE_FATHER, ARITY_FATHER);
        father1.args[0] = new Constant(CONSTANT_ID, "tom");
        father1.args[1] = new Constant(CONSTANT_ID, "jerry");
        Predicate father2 = new Predicate(TABLE_FATHER, ARITY_FATHER);
        father2.args[0] = new Constant(CONSTANT_ID, "jerry");
        father2.args[1] = new Constant(CONSTANT_ID, "laura");
        Set<Predicate> father_set = new HashSet<>();
        father_set.add(father1);
        System.out.println(father1);
        father_set.add(father2);
        System.out.println(father2);
        assertEquals(father_set, kb.listPredicate(TABLE_FATHER, ARITY_FATHER));

        Predicate mother1 = new Predicate(TABLE_MOTHER, ARITY_MOTHER);
        mother1.args[0] = new Constant(CONSTANT_ID, "amie");
        mother1.args[1] = new Constant(CONSTANT_ID, "laura");
        Predicate mother2 = new Predicate(TABLE_MOTHER, ARITY_MOTHER);
        mother2.args[0] = new Constant(CONSTANT_ID, "amie");
        mother2.args[1] = new Constant(CONSTANT_ID, "bob");
        Set<Predicate> mother_set = new HashSet<>();
        mother_set.add(mother1);
        System.out.println(mother1);
        mother_set.add(mother2);
        System.out.println(mother2);
        assertEquals(mother_set, kb.listPredicate(TABLE_MOTHER, ARITY_MOTHER));
        System.out.println();

        /* 交通图KB */
        System.out.println("交通图KB");
        Predicate road1 = new Predicate(TABLE_ROAD, ARITY_ROAD);
        road1.args[0] = new Constant(CONSTANT_ID, "济青高速");
        road1.args[1] = new Constant(CONSTANT_ID, "济南");
        road1.args[2] = new Constant(CONSTANT_ID, "东");
        road1.args[3] = new Constant(CONSTANT_ID, "青岛");
        road1.args[4] = new Constant(CONSTANT_ID, "西");
        Predicate road2 = new Predicate(TABLE_ROAD, ARITY_ROAD);
        road2.args[0] = new Constant(CONSTANT_ID, "胶济铁路");
        road2.args[1] = new Constant(CONSTANT_ID, "济南");
        road2.args[2] = new Constant(CONSTANT_ID, "南");
        road2.args[3] = new Constant(CONSTANT_ID, "胶州湾");
        road2.args[4] = new Constant(CONSTANT_ID, "南");
        Predicate road3 = new Predicate(TABLE_ROAD, ARITY_ROAD);
        road3.args[0] = new Constant(CONSTANT_ID, "上海内环");
        road3.args[1] = new Constant(CONSTANT_ID, "上海");
        road3.args[2] = new Constant(CONSTANT_ID, "北");
        road3.args[3] = new Constant(CONSTANT_ID, "上海");
        road3.args[4] = new Constant(CONSTANT_ID, "东");
        Predicate road4 = new Predicate(TABLE_ROAD, ARITY_ROAD);
        road4.args[0] = new Constant(CONSTANT_ID, "上海外环");
        road4.args[1] = new Constant(CONSTANT_ID, "上海");
        road4.args[2] = new Constant(CONSTANT_ID, "南");
        road4.args[3] = new Constant(CONSTANT_ID, "上海");
        road4.args[4] = new Constant(CONSTANT_ID, "南");
        Set<Predicate> road_set = new HashSet<>();
        road_set.add(road1);
        System.out.println(road1);
        road_set.add(road2);
        System.out.println(road2);
        road_set.add(road3);
        System.out.println(road3);
        road_set.add(road4);
        System.out.println(road4);
        assertEquals(road_set, kb.listPredicate(TABLE_ROAD, ARITY_ROAD));

        Predicate road_type1 = new Predicate(TABLE_ROAD_TYPE, ARITY_ROAD_TYPE);
        road_type1.args[0] = new Constant(CONSTANT_ID, "济青高速");
        road_type1.args[1] = new Constant(CONSTANT_ID, "公路");
        road_type1.args[2] = new Constant(CONSTANT_ID, "直");
        Predicate road_type2 = new Predicate(TABLE_ROAD_TYPE, ARITY_ROAD_TYPE);
        road_type2.args[0] = new Constant(CONSTANT_ID, "胶济铁路");
        road_type2.args[1] = new Constant(CONSTANT_ID, "铁路");
        road_type2.args[2] = new Constant(CONSTANT_ID, "弯");
        Predicate road_type3 = new Predicate(TABLE_ROAD_TYPE, ARITY_ROAD_TYPE);
        road_type3.args[0] = new Constant(CONSTANT_ID, "上海内环");
        road_type3.args[1] = new Constant(CONSTANT_ID, "公路");
        road_type3.args[2] = new Constant(CONSTANT_ID, "环");
        Predicate road_type4 = new Predicate(TABLE_ROAD_TYPE, ARITY_ROAD_TYPE);
        road_type4.args[0] = new Constant(CONSTANT_ID, "上海外环");
        road_type4.args[1] = new Constant(CONSTANT_ID, "公路");
        road_type4.args[2] = new Constant(CONSTANT_ID, "环");
        Set<Predicate> road_type_set = new HashSet<>();
        road_type_set.add(road_type1);
        System.out.println(road_type1);
        road_type_set.add(road_type2);
        System.out.println(road_type2);
        road_type_set.add(road_type3);
        System.out.println(road_type3);
        road_type_set.add(road_type4);
        System.out.println(road_type4);
        assertEquals(road_type_set, kb.listPredicate(TABLE_ROAD_TYPE, ARITY_ROAD_TYPE));
        System.out.println();
    }
}