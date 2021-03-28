package sinc.util;

import org.junit.jupiter.api.*;
import sinc.common.Constant;
import sinc.common.Eval;
import sinc.common.Predicate;
import sinc.common.Rule;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RKBTest {
    static final String TABLE_LINKED = "linked";
    static final int ARITY_LINKED = 2;
    static final String TABLE_CONNECTED = "connected";
    static final String TABLE_CONNECTED_PROVED = TABLE_CONNECTED + RKB.PROVED_TABLE_NAME_SUFFIX;
    static final int ARITY_CONNECTED = 2;
    static final String TABLE_MALE = "male";
    static final String TABLE_MALE_PROVED = TABLE_MALE + RKB.PROVED_TABLE_NAME_SUFFIX;
    static final int ARITY_MALE = 1;
    static final String TABLE_FEMALE = "female";
    static final String TABLE_FEMALE_PROVED = TABLE_FEMALE + RKB.PROVED_TABLE_NAME_SUFFIX;
    static final int ARITY_FEMALE = 1;
    static final String TABLE_FATHER = "father";
    static final String TABLE_FATHER_PROVED = TABLE_FATHER + RKB.PROVED_TABLE_NAME_SUFFIX;
    static final int ARITY_FATHER = 2;
    static final String TABLE_MOTHER = "mother";
    static final int ARITY_MOTHER = 2;
    static final String TABLE_ROAD = "road";
    static final String TABLE_ROAD_PROVED = TABLE_ROAD + RKB.PROVED_TABLE_NAME_SUFFIX;
    static final int ARITY_ROAD = 5;
    static final String TABLE_ROAD_TYPE = "roadType";
    static final int ARITY_ROAD_TYPE = 3;
    static final int CONSTANT_ID = RKB.CONSTANT_ID;

    static final RKB kb;
    static final Connection connection;

    static {
        RKB tmp;
        try {
            tmp = new RKB(null);
        } catch (Exception e) {
            tmp = null;
        }
        kb = tmp;

        Connection con;
        try {
            Field field_connected = RKB.class.getDeclaredField("connection");
            field_connected.setAccessible(true);
            con = (Connection) field_connected.get(kb);
        } catch (Exception e) {
            con = null;
        }
        connection = con;
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

        Predicate connected_proved1 = new Predicate(TABLE_CONNECTED_PROVED, ARITY_CONNECTED);
        connected_proved1.args[0] = new Constant(CONSTANT_ID, "a");
        connected_proved1.args[1] = new Constant(CONSTANT_ID, "b");
        Predicate connected_proved2 = new Predicate(TABLE_CONNECTED_PROVED, ARITY_CONNECTED);
        connected_proved2.args[0] = new Constant(CONSTANT_ID, "a");
        connected_proved2.args[1] = new Constant(CONSTANT_ID, "d");
        Predicate connected_proved3 = new Predicate(TABLE_CONNECTED_PROVED, ARITY_CONNECTED);
        connected_proved3.args[0] = new Constant(CONSTANT_ID, "f");
        connected_proved3.args[1] = new Constant(CONSTANT_ID, "d");

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
        kb.addPredicate(connected_proved1);
        kb.addPredicate(connected_proved2);
        kb.addPredicate(connected_proved3);

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
    @Order(1)
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

        Predicate connected_proved1 = new Predicate(TABLE_CONNECTED_PROVED, ARITY_CONNECTED);
        connected_proved1.args[0] = new Constant(CONSTANT_ID, "a");
        connected_proved1.args[1] = new Constant(CONSTANT_ID, "b");
        Predicate connected_proved2 = new Predicate(TABLE_CONNECTED_PROVED, ARITY_CONNECTED);
        connected_proved2.args[0] = new Constant(CONSTANT_ID, "a");
        connected_proved2.args[1] = new Constant(CONSTANT_ID, "d");
        Predicate connected_proved3 = new Predicate(TABLE_CONNECTED_PROVED, ARITY_CONNECTED);
        connected_proved3.args[0] = new Constant(CONSTANT_ID, "f");
        connected_proved3.args[1] = new Constant(CONSTANT_ID, "d");
        Set<Predicate> connected_proved_set = new HashSet<>();
        connected_proved_set.add(connected_proved1);
        System.out.println(connected_proved1);
        connected_proved_set.add(connected_proved2);
        System.out.println(connected_proved2);
        connected_proved_set.add(connected_proved3);
        System.out.println(connected_proved3);
        assertEquals(connected_proved_set, kb.listPredicate(TABLE_CONNECTED_PROVED, ARITY_CONNECTED));
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

    @Test
    @Order(2)
    void testGraph1() throws Exception {
        Rule rule = new Rule(TABLE_CONNECTED, ARITY_CONNECTED);
        assertEquals("(null)" + TABLE_CONNECTED + "(?,?):-", rule.toString());
        String sql4all = getSql4AllEntailments(rule);
        String sql4new_pos = getSql4UnprovedPosEntailments(rule);
        Eval eval = kb.evalRule(rule);

        assertNull(sql4all);
        assertEquals(
                String.format(
                    "SELECT DISTINCT %s0.C0 AS C0,%s0.C1 AS C1 " +
                            "FROM %s AS %s0 " +
                            "EXCEPT SELECT * FROM %s",
                        TABLE_CONNECTED, TABLE_CONNECTED, TABLE_CONNECTED, TABLE_CONNECTED,
                        TABLE_CONNECTED_PROVED
                ), sql4new_pos
        );
        assertEquals(new Eval(6, 784, 0), eval);

        Predicate predicate1 = new Predicate(TABLE_CONNECTED, ARITY_CONNECTED);
        predicate1.args[0] = new Constant(CONSTANT_ID, "a");
        predicate1.args[1] = new Constant(CONSTANT_ID, "b");
        Predicate predicate2 = new Predicate(TABLE_CONNECTED, ARITY_CONNECTED);
        predicate2.args[0] = new Constant(CONSTANT_ID, "b");
        predicate2.args[1] = new Constant(CONSTANT_ID, "c");
        Predicate predicate3 = new Predicate(TABLE_CONNECTED, ARITY_CONNECTED);
        predicate3.args[0] = new Constant(CONSTANT_ID, "a");
        predicate3.args[1] = new Constant(CONSTANT_ID, "d");
        Predicate predicate4 = new Predicate(TABLE_CONNECTED, ARITY_CONNECTED);
        predicate4.args[0] = new Constant(CONSTANT_ID, "e");
        predicate4.args[1] = new Constant(CONSTANT_ID, "a");
        Predicate predicate5 = new Predicate(TABLE_CONNECTED, ARITY_CONNECTED);
        predicate5.args[0] = new Constant(CONSTANT_ID, "a");
        predicate5.args[1] = new Constant(CONSTANT_ID, "c");
        Predicate predicate6 = new Predicate(TABLE_CONNECTED, ARITY_CONNECTED);
        predicate6.args[0] = new Constant(CONSTANT_ID, "e");
        predicate6.args[1] = new Constant(CONSTANT_ID, "c");
        Predicate predicate7 = new Predicate(TABLE_CONNECTED, ARITY_CONNECTED);
        predicate7.args[0] = new Constant(CONSTANT_ID, "e");
        predicate7.args[1] = new Constant(CONSTANT_ID, "b");
        Predicate predicate8 = new Predicate(TABLE_CONNECTED, ARITY_CONNECTED);
        predicate8.args[0] = new Constant(CONSTANT_ID, "e");
        predicate8.args[1] = new Constant(CONSTANT_ID, "d");
        Set<Predicate> new_pos_preds = new HashSet<>();
        new_pos_preds.add(predicate2);
        new_pos_preds.add(predicate4);
        new_pos_preds.add(predicate5);
        new_pos_preds.add(predicate6);
        new_pos_preds.add(predicate7);
        new_pos_preds.add(predicate8);
        assertEquals(new_pos_preds, query4Predicates(sql4new_pos, TABLE_CONNECTED, ARITY_CONNECTED));

        assertEquals(
                String.format(
                        "SELECT DISTINCT %s0.C0 AS X0,%s0.C1 AS X1 " +
                                "FROM %s AS %s0 " +
                                "GROUP BY X0,X1",
                        TABLE_CONNECTED, TABLE_CONNECTED,
                        TABLE_CONNECTED, TABLE_CONNECTED
                ), getSql4Groundings(rule)
        );
        Set<Predicate[]> grounding_set = new HashSet<>();
        grounding_set.add(new Predicate[]{predicate1});
        grounding_set.add(new Predicate[]{predicate2});
        grounding_set.add(new Predicate[]{predicate3});
        grounding_set.add(new Predicate[]{predicate4});
        grounding_set.add(new Predicate[]{predicate5});
        grounding_set.add(new Predicate[]{predicate6});
        grounding_set.add(new Predicate[]{predicate7});
        grounding_set.add(new Predicate[]{predicate8});
        assertEqualOfArraySets(grounding_set, new HashSet<>(kb.findGroundings(rule)));

        assertEquals(776, kb.findCounterExamples(rule).size());
    }

    @Test
    @Order(3)
    void testGraph2() throws Exception {
        Rule rule = new Rule(TABLE_CONNECTED, ARITY_CONNECTED);
        rule.boundFreeVar2Constant(0, 0, CONSTANT_ID, "e");
        assertEquals(String.format("(null)%s(e,?):-", TABLE_CONNECTED), rule.toString());
        String sql4all = getSql4AllEntailments(rule);
        String sql4new_pos = getSql4UnprovedPosEntailments(rule);
        Eval eval = kb.evalRule(rule);

        assertNull(sql4all);
        assertEquals(
                String.format(
                        "SELECT DISTINCT %s0.C0 AS C0,%s0.C1 AS C1 " +
                                "FROM %s AS %s0 " +
                                "WHERE %s0.C0='e' " +
                                "EXCEPT SELECT * FROM %s",
                        TABLE_CONNECTED, TABLE_CONNECTED, TABLE_CONNECTED, TABLE_CONNECTED,
                        TABLE_CONNECTED, TABLE_CONNECTED_PROVED
                ), sql4new_pos
        );
        assertEquals(new Eval(4, 28, 1), eval);

        Predicate predicate1 = new Predicate(TABLE_CONNECTED, ARITY_CONNECTED);
        predicate1.args[0] = new Constant(CONSTANT_ID, "e");
        predicate1.args[1] = new Constant(CONSTANT_ID, "a");
        Predicate predicate2 = new Predicate(TABLE_CONNECTED, ARITY_CONNECTED);
        predicate2.args[0] = new Constant(CONSTANT_ID, "e");
        predicate2.args[1] = new Constant(CONSTANT_ID, "c");
        Predicate predicate3 = new Predicate(TABLE_CONNECTED, ARITY_CONNECTED);
        predicate3.args[0] = new Constant(CONSTANT_ID, "e");
        predicate3.args[1] = new Constant(CONSTANT_ID, "b");
        Predicate predicate4 = new Predicate(TABLE_CONNECTED, ARITY_CONNECTED);
        predicate4.args[0] = new Constant(CONSTANT_ID, "e");
        predicate4.args[1] = new Constant(CONSTANT_ID, "d");
        Set<Predicate> new_pos_preds = new HashSet<>();
        new_pos_preds.add(predicate1);
        new_pos_preds.add(predicate2);
        new_pos_preds.add(predicate3);
        new_pos_preds.add(predicate4);
        assertEquals(new_pos_preds, query4Predicates(sql4new_pos, TABLE_CONNECTED, ARITY_CONNECTED));

        assertEquals(
                String.format(
                        "SELECT DISTINCT %s0.C1 AS X0 " +
                                "FROM %s AS %s0 " +
                                "WHERE %s0.C0='e' " +
                                "GROUP BY X0",
                        TABLE_CONNECTED,
                        TABLE_CONNECTED, TABLE_CONNECTED,
                        TABLE_CONNECTED
                ), getSql4Groundings(rule)
        );
        Set<Predicate[]> grounding_set = new HashSet<>();
        grounding_set.add(new Predicate[]{predicate1});
        grounding_set.add(new Predicate[]{predicate2});
        grounding_set.add(new Predicate[]{predicate3});
        grounding_set.add(new Predicate[]{predicate4});
        assertEqualOfArraySets(grounding_set, new HashSet<>(kb.findGroundings(rule)));

        assertEquals(24, kb.findCounterExamples(rule).size());
    }

    @Test
    @Order(4)
    void testGraph3() throws Exception{
        Rule rule = new Rule(TABLE_CONNECTED, ARITY_CONNECTED);
        rule.boundFreeVars2NewVar(0, 0, 0, 1);
        assertEquals(String.format("(null)%s(X0,X0):-", TABLE_CONNECTED), rule.toString());
        String sql4all = getSql4AllEntailments(rule);
        String sql4new_pos = getSql4UnprovedPosEntailments(rule);
        Eval eval = kb.evalRule(rule);

        assertNull(sql4all);
        assertEquals(
                String.format(
                        "SELECT DISTINCT %s0.C0 AS C0,%s0.C1 AS C1 " +
                                "FROM %s AS %s0 " +
                                "WHERE %s0.C0=%s0.C1 " +
                                "EXCEPT SELECT * FROM %s",
                        TABLE_CONNECTED, TABLE_CONNECTED, TABLE_CONNECTED, TABLE_CONNECTED,
                        TABLE_CONNECTED, TABLE_CONNECTED, TABLE_CONNECTED_PROVED
                ), sql4new_pos
        );
        assertEquals(new Eval(0, 28, 1), eval);
        assertTrue(query4Predicates(sql4new_pos, TABLE_CONNECTED, ARITY_CONNECTED).isEmpty());

        assertEquals(
                String.format(
                        "SELECT DISTINCT %s0.C0 AS X0 " +
                                "FROM %s AS %s0 " +
                                "WHERE %s0.C0=%s0.C1 " +
                                "GROUP BY X0",
                        TABLE_CONNECTED,
                        TABLE_CONNECTED, TABLE_CONNECTED,
                        TABLE_CONNECTED, TABLE_CONNECTED
                ), getSql4Groundings(rule)
        );
        assertTrue(kb.findGroundings(rule).isEmpty());

        assertEquals(28, kb.findCounterExamples(rule).size());
    }

    @Test
    @Order(5)
    void testGraph4() throws Exception {
        Rule rule = new Rule(TABLE_CONNECTED, ARITY_CONNECTED);
        rule.addPred(TABLE_LINKED, ARITY_LINKED);
        rule.boundFreeVars2NewVar(0, 0, 1, 0);
        rule.boundFreeVars2NewVar(0, 1, 1, 1);
        assertEquals(
                String.format("(null)%s(X0,X1):-%s(X0,X1)", TABLE_CONNECTED, TABLE_LINKED),
                rule.toString()
        );
        String sql4all = getSql4AllEntailments(rule);
        String sql4new_pos = getSql4UnprovedPosEntailments(rule);
        Eval eval = kb.evalRule(rule);

        assertEquals(
                String.format(
                        "SELECT DISTINCT %s1.C0 AS X0,%s1.C1 AS X1 " +
                                "FROM %s AS %s1 ",
                        TABLE_LINKED, TABLE_LINKED,
                        TABLE_LINKED, TABLE_LINKED
                ), sql4all
        );
        assertEquals(
                String.format(
                        "SELECT DISTINCT %s0.C0 AS C0,%s0.C1 AS C1 " +
                                "FROM %s AS %s0,%s AS %s1 " +
                                "WHERE %s0.C0=%s1.C0 AND %s0.C1=%s1.C1 " +
                                "EXCEPT SELECT * FROM %s",
                        TABLE_CONNECTED, TABLE_CONNECTED,
                        TABLE_CONNECTED, TABLE_CONNECTED, TABLE_LINKED, TABLE_LINKED,
                        TABLE_CONNECTED, TABLE_LINKED, TABLE_CONNECTED, TABLE_LINKED,
                        TABLE_CONNECTED_PROVED
                ), sql4new_pos
        );
        assertEquals(new Eval(2, 4, 2), eval);

        Predicate predicate1 = new Predicate(TABLE_CONNECTED, ARITY_CONNECTED);
        predicate1.args[0] = new Constant(CONSTANT_ID, "a");
        predicate1.args[1] = new Constant(CONSTANT_ID, "b");
        Predicate predicate2 = new Predicate(TABLE_CONNECTED, ARITY_CONNECTED);
        predicate2.args[0] = new Constant(CONSTANT_ID, "b");
        predicate2.args[1] = new Constant(CONSTANT_ID, "c");
        Predicate predicate3 = new Predicate(TABLE_CONNECTED, ARITY_CONNECTED);
        predicate3.args[0] = new Constant(CONSTANT_ID, "a");
        predicate3.args[1] = new Constant(CONSTANT_ID, "d");
        Predicate predicate4 = new Predicate(TABLE_CONNECTED, ARITY_CONNECTED);
        predicate4.args[0] = new Constant(CONSTANT_ID, "e");
        predicate4.args[1] = new Constant(CONSTANT_ID, "a");
        Set<Predicate> new_pos_preds = new HashSet<>();
        new_pos_preds.add(predicate2);
        new_pos_preds.add(predicate4);
        assertEquals(new_pos_preds, query4Predicates(sql4new_pos, TABLE_CONNECTED, ARITY_CONNECTED));

        assertEquals(
                String.format(
                        "SELECT DISTINCT %s0.C0 AS X0,%s0.C1 AS X1 " +
                                "FROM %s AS %s0,%s AS %s1 " +
                                "WHERE %s0.C0=%s1.C0 AND %s0.C1=%s1.C1 " +
                                "GROUP BY X0,X1",
                        TABLE_CONNECTED, TABLE_CONNECTED,
                        TABLE_CONNECTED, TABLE_CONNECTED, TABLE_LINKED, TABLE_LINKED,
                        TABLE_CONNECTED, TABLE_LINKED, TABLE_CONNECTED, TABLE_LINKED
                ), getSql4Groundings(rule)
        );
        Predicate body_pred1 = new Predicate(TABLE_LINKED, ARITY_LINKED);
        body_pred1.args[0] = new Constant(CONSTANT_ID, "a");
        body_pred1.args[1] = new Constant(CONSTANT_ID, "b");
        Predicate body_pred2 = new Predicate(TABLE_LINKED, ARITY_LINKED);
        body_pred2.args[0] = new Constant(CONSTANT_ID, "b");
        body_pred2.args[1] = new Constant(CONSTANT_ID, "c");
        Predicate body_pred3 = new Predicate(TABLE_LINKED, ARITY_LINKED);
        body_pred3.args[0] = new Constant(CONSTANT_ID, "a");
        body_pred3.args[1] = new Constant(CONSTANT_ID, "d");
        Predicate body_pred4 = new Predicate(TABLE_LINKED, ARITY_LINKED);
        body_pred4.args[0] = new Constant(CONSTANT_ID, "e");
        body_pred4.args[1] = new Constant(CONSTANT_ID, "a");

        Set<Predicate[]> grounding_set = new HashSet<>();
        grounding_set.add(new Predicate[]{predicate1, body_pred1});
        grounding_set.add(new Predicate[]{predicate2, body_pred2});
        grounding_set.add(new Predicate[]{predicate3, body_pred3});
        grounding_set.add(new Predicate[]{predicate4, body_pred4});
        assertEqualOfArraySets(grounding_set, new HashSet<>(kb.findGroundings(rule)));

        assertTrue(kb.findCounterExamples(rule).isEmpty());
    }

    @Test
    @Order(6)
    void testGraph5() throws Exception {
        Rule rule = new Rule(TABLE_CONNECTED, ARITY_CONNECTED);
        rule.addPred(TABLE_LINKED, ARITY_LINKED);
        rule.boundFreeVars2NewVar(0, 0, 1, 0);
        rule.addPred(TABLE_LINKED, ARITY_LINKED);
        rule.boundFreeVars2NewVar(0, 1, 2, 1);
        rule.boundFreeVars2NewVar(1, 1, 2, 0);
        assertEquals(
                String.format("(null)%s(X0,X1):-%s(X0,X2),%s(X2,X1)",
                        TABLE_CONNECTED, TABLE_LINKED, TABLE_LINKED
                ), rule.toString()
        );
        String sql4all = getSql4AllEntailments(rule);
        String sql4new_pos = getSql4UnprovedPosEntailments(rule);
        Eval eval = kb.evalRule(rule);

        assertEquals(
                String.format(
                        "SELECT DISTINCT %s1.C0 AS X0,%s2.C1 AS X1 " +
                                "FROM %s AS %s1,%s AS %s2 " +
                                "WHERE %s1.C1=%s2.C0",
                        TABLE_LINKED, TABLE_LINKED,
                        TABLE_LINKED, TABLE_LINKED, TABLE_LINKED, TABLE_LINKED,
                        TABLE_LINKED, TABLE_LINKED
                ), sql4all
        );
        assertEquals(
                String.format(
                        "SELECT DISTINCT %s0.C0 AS C0,%s0.C1 AS C1 " +
                                "FROM %s AS %s0,%s AS %s1,%s AS %s2 " +
                                "WHERE %s0.C0=%s1.C0 AND %s1.C1=%s2.C0 AND %s0.C1=%s2.C1 " +
                                "EXCEPT SELECT * FROM %s",
                        TABLE_CONNECTED, TABLE_CONNECTED,
                        TABLE_CONNECTED, TABLE_CONNECTED, TABLE_LINKED, TABLE_LINKED, TABLE_LINKED, TABLE_LINKED,
                        TABLE_CONNECTED, TABLE_LINKED, TABLE_LINKED, TABLE_LINKED, TABLE_CONNECTED, TABLE_LINKED,
                        TABLE_CONNECTED_PROVED
                ), sql4new_pos
        );
        assertEquals(new Eval(3, 3, 3), eval);

        Predicate predicate1 = new Predicate(TABLE_CONNECTED, ARITY_CONNECTED);
        predicate1.args[0] = new Constant(CONSTANT_ID, "a");
        predicate1.args[1] = new Constant(CONSTANT_ID, "c");
        Predicate predicate2 = new Predicate(TABLE_CONNECTED, ARITY_CONNECTED);
        predicate2.args[0] = new Constant(CONSTANT_ID, "e");
        predicate2.args[1] = new Constant(CONSTANT_ID, "b");
        Predicate predicate3 = new Predicate(TABLE_CONNECTED, ARITY_CONNECTED);
        predicate3.args[0] = new Constant(CONSTANT_ID, "e");
        predicate3.args[1] = new Constant(CONSTANT_ID, "d");
        Set<Predicate> new_pos_preds = new HashSet<>();
        new_pos_preds.add(predicate1);
        new_pos_preds.add(predicate2);
        new_pos_preds.add(predicate3);
        assertEquals(new_pos_preds, query4Predicates(sql4new_pos, TABLE_CONNECTED, ARITY_CONNECTED));

        assertEquals(
                String.format(
                        "SELECT DISTINCT %s0.C0 AS X0,%s0.C1 AS X1,%s1.C1 AS X2 " +
                                "FROM %s AS %s0,%s AS %s1,%s AS %s2 " +
                                "WHERE %s0.C0=%s1.C0 AND %s1.C1=%s2.C0 AND %s0.C1=%s2.C1 " +
                                "GROUP BY X0,X1",
                        TABLE_CONNECTED, TABLE_CONNECTED, TABLE_LINKED,
                        TABLE_CONNECTED, TABLE_CONNECTED, TABLE_LINKED, TABLE_LINKED, TABLE_LINKED, TABLE_LINKED,
                        TABLE_CONNECTED, TABLE_LINKED, TABLE_LINKED, TABLE_LINKED, TABLE_CONNECTED, TABLE_LINKED
                ), getSql4Groundings(rule)
        );
        Predicate body_pred1 = new Predicate(TABLE_LINKED, ARITY_LINKED);
        body_pred1.args[0] = new Constant(CONSTANT_ID, "a");
        body_pred1.args[1] = new Constant(CONSTANT_ID, "b");
        Predicate body_pred2 = new Predicate(TABLE_LINKED, ARITY_LINKED);
        body_pred2.args[0] = new Constant(CONSTANT_ID, "b");
        body_pred2.args[1] = new Constant(CONSTANT_ID, "c");
        Predicate body_pred3 = new Predicate(TABLE_LINKED, ARITY_LINKED);
        body_pred3.args[0] = new Constant(CONSTANT_ID, "a");
        body_pred3.args[1] = new Constant(CONSTANT_ID, "d");
        Predicate body_pred4 = new Predicate(TABLE_LINKED, ARITY_LINKED);
        body_pred4.args[0] = new Constant(CONSTANT_ID, "e");
        body_pred4.args[1] = new Constant(CONSTANT_ID, "a");

        Set<Predicate[]> grounding_set = new HashSet<>();
        grounding_set.add(new Predicate[]{predicate1, body_pred1, body_pred2});
        grounding_set.add(new Predicate[]{predicate2, body_pred4, body_pred1});
        grounding_set.add(new Predicate[]{predicate3, body_pred4, body_pred3});
        assertEqualOfArraySets(grounding_set, new HashSet<>(kb.findGroundings(rule)));

        assertTrue(kb.findCounterExamples(rule).isEmpty());
    }

    @Test
    @Order(7)
    void testFamily1() throws Exception {
        Rule rule = new Rule(TABLE_MALE, ARITY_MALE);
        rule.addPred(TABLE_FATHER, ARITY_FATHER);
        rule.boundFreeVars2NewVar(0, 0, 1, 0);
        assertEquals(
                String.format("(null)%s(X0):-%s(X0,?)",
                        TABLE_MALE, TABLE_FATHER
                ), rule.toString()
        );
        String sql4all = getSql4AllEntailments(rule);
        String sql4new_pos = getSql4UnprovedPosEntailments(rule);
        Eval eval = kb.evalRule(rule);

        assertEquals(
                String.format(
                        "SELECT DISTINCT %s1.C0 AS X0 " +
                                "FROM %s AS %s1 ",
                        TABLE_FATHER,
                        TABLE_FATHER, TABLE_FATHER
                ), sql4all
        );
        assertEquals(
                String.format(
                        "SELECT DISTINCT %s0.C0 AS C0 " +
                                "FROM %s AS %s0,%s AS %s1 " +
                                "WHERE %s0.C0=%s1.C0 " +
                                "EXCEPT SELECT * FROM %s",
                        TABLE_MALE,
                        TABLE_MALE, TABLE_MALE, TABLE_FATHER, TABLE_FATHER,
                        TABLE_MALE, TABLE_FATHER,
                        TABLE_MALE_PROVED
                ), sql4new_pos
        );
        assertEquals(new Eval(2, 2, 1), eval);

        Predicate predicate1 = new Predicate(TABLE_MALE, ARITY_MALE);
        predicate1.args[0] = new Constant(CONSTANT_ID, "tom");
        Predicate predicate2 = new Predicate(TABLE_MALE, ARITY_MALE);
        predicate2.args[0] = new Constant(CONSTANT_ID, "jerry");
        Set<Predicate> new_pos_preds = new HashSet<>();
        new_pos_preds.add(predicate1);
        new_pos_preds.add(predicate2);
        assertEquals(new_pos_preds, query4Predicates(sql4new_pos, TABLE_MALE, ARITY_MALE));

        assertEquals(
                String.format(
                        "SELECT DISTINCT %s0.C0 AS X0,%s1.C1 AS X1 " +
                                "FROM %s AS %s0,%s AS %s1 " +
                                "WHERE %s0.C0=%s1.C0 " +
                                "GROUP BY X0",
                        TABLE_MALE, TABLE_FATHER,
                        TABLE_MALE, TABLE_MALE, TABLE_FATHER, TABLE_FATHER,
                        TABLE_MALE, TABLE_FATHER
                ), getSql4Groundings(rule)
        );
        Predicate body_pred1 = new Predicate(TABLE_FATHER, ARITY_FATHER);
        body_pred1.args[0] = new Constant(CONSTANT_ID, "tom");
        body_pred1.args[1] = new Constant(CONSTANT_ID, "jerry");
        Predicate body_pred2 = new Predicate(TABLE_FATHER, ARITY_FATHER);
        body_pred2.args[0] = new Constant(CONSTANT_ID, "jerry");
        body_pred2.args[1] = new Constant(CONSTANT_ID, "laura");

        Set<Predicate[]> grounding_set = new HashSet<>();
        grounding_set.add(new Predicate[]{predicate1, body_pred1});
        grounding_set.add(new Predicate[]{predicate2, body_pred2});
        assertEqualOfArraySets(grounding_set, new HashSet<>(kb.findGroundings(rule)));

        assertTrue(kb.findCounterExamples(rule).isEmpty());
    }

    @Test
    @Order(8)
    void testFamily2() throws Exception {
        Rule rule = new Rule(TABLE_FEMALE, ARITY_FEMALE);
        rule.addPred(TABLE_MOTHER, ARITY_MOTHER);
        rule.boundFreeVars2NewVar(0, 0, 1, 0);
        assertEquals(
                String.format("(null)%s(X0):-%s(X0,?)",
                        TABLE_FEMALE, TABLE_MOTHER
                ), rule.toString()
        );
        String sql4all = getSql4AllEntailments(rule);
        String sql4new_pos = getSql4UnprovedPosEntailments(rule);
        Eval eval = kb.evalRule(rule);

        assertEquals(
                String.format(
                        "SELECT DISTINCT %s1.C0 AS X0 " +
                                "FROM %s AS %s1 ",
                        TABLE_MOTHER,
                        TABLE_MOTHER, TABLE_MOTHER
                ), sql4all
        );
        assertEquals(
                String.format(
                        "SELECT DISTINCT %s0.C0 AS C0 " +
                                "FROM %s AS %s0,%s AS %s1 " +
                                "WHERE %s0.C0=%s1.C0 " +
                                "EXCEPT SELECT * FROM %s",
                        TABLE_FEMALE,
                        TABLE_FEMALE, TABLE_FEMALE, TABLE_MOTHER, TABLE_MOTHER,
                        TABLE_FEMALE, TABLE_MOTHER,
                        TABLE_FEMALE_PROVED
                ), sql4new_pos
        );
        assertEquals(new Eval(1, 1, 1), eval);

        Predicate predicate1 = new Predicate(TABLE_FEMALE, ARITY_FEMALE);
        predicate1.args[0] = new Constant(CONSTANT_ID, "amie");
        Set<Predicate> new_pos_preds = new HashSet<>();
        new_pos_preds.add(predicate1);
        assertEquals(new_pos_preds, query4Predicates(sql4new_pos, TABLE_FEMALE, ARITY_FEMALE));

        assertEquals(
                String.format(
                        "SELECT DISTINCT %s0.C0 AS X0,%s1.C1 AS X1 " +
                                "FROM %s AS %s0,%s AS %s1 " +
                                "WHERE %s0.C0=%s1.C0 " +
                                "GROUP BY X0",
                        TABLE_FEMALE, TABLE_MOTHER,
                        TABLE_FEMALE, TABLE_FEMALE, TABLE_MOTHER, TABLE_MOTHER,
                        TABLE_FEMALE, TABLE_MOTHER
                ), getSql4Groundings(rule)
        );
        Predicate body_pred1 = new Predicate(TABLE_MOTHER, ARITY_MOTHER);
        body_pred1.args[0] = new Constant(CONSTANT_ID, "amie");
        body_pred1.args[1] = new Constant(CONSTANT_ID, "laura");
        Predicate body_pred2 = new Predicate(TABLE_MOTHER, ARITY_MOTHER);
        body_pred2.args[0] = new Constant(CONSTANT_ID, "amie");
        body_pred2.args[1] = new Constant(CONSTANT_ID, "bob");

        Set<Predicate[]> grounding_set = new HashSet<>();
        grounding_set.add(new Predicate[]{predicate1, body_pred1});
        grounding_set.add(new Predicate[]{predicate1, body_pred2});
        Set<Predicate[]> actual_grounding_set = new HashSet<>(kb.findGroundings(rule));
        assertEquals(1, actual_grounding_set.size());
        for (Predicate[] grounding: actual_grounding_set) {
            boolean found = false;
            for (Predicate[] posible_grounding: grounding_set) {
                if (Arrays.equals(posible_grounding, grounding)) {
                    found = true;
                    break;
                }
            }
            assertTrue(found);
        }

        assertTrue(kb.findCounterExamples(rule).isEmpty());
    }

    @Test
    @Order(9)
    void testFamily3() throws Exception {
        Rule rule = new Rule(TABLE_MALE, ARITY_MALE);
        rule.addPred(TABLE_MOTHER, ARITY_MOTHER);
        rule.boundFreeVars2NewVar(0, 0, 1, 0);
        assertEquals(
                String.format("(null)%s(X0):-%s(X0,?)",
                        TABLE_MALE, TABLE_MOTHER
                ), rule.toString()
        );
        String sql4all = getSql4AllEntailments(rule);
        String sql4new_pos = getSql4UnprovedPosEntailments(rule);
        Eval eval = kb.evalRule(rule);

        assertEquals(
                String.format(
                        "SELECT DISTINCT %s1.C0 AS X0 " +
                                "FROM %s AS %s1 ",
                        TABLE_MOTHER,
                        TABLE_MOTHER, TABLE_MOTHER
                ), sql4all
        );
        assertEquals(
                String.format(
                        "SELECT DISTINCT %s0.C0 AS C0 " +
                                "FROM %s AS %s0,%s AS %s1 " +
                                "WHERE %s0.C0=%s1.C0 " +
                                "EXCEPT SELECT * FROM %s",
                        TABLE_MALE,
                        TABLE_MALE, TABLE_MALE, TABLE_MOTHER, TABLE_MOTHER,
                        TABLE_MALE, TABLE_MOTHER,
                        TABLE_MALE_PROVED
                ), sql4new_pos
        );
        assertEquals(new Eval(0, 1, 1), eval);
        assertTrue(query4Predicates(sql4new_pos, TABLE_MALE, ARITY_MALE).isEmpty());

        assertEquals(
                String.format(
                        "SELECT DISTINCT %s0.C0 AS X0,%s1.C1 AS X1 " +
                                "FROM %s AS %s0,%s AS %s1 " +
                                "WHERE %s0.C0=%s1.C0 " +
                                "GROUP BY X0",
                        TABLE_MALE, TABLE_MOTHER,
                        TABLE_MALE, TABLE_MALE, TABLE_MOTHER, TABLE_MOTHER,
                        TABLE_MALE, TABLE_MOTHER
                ), getSql4Groundings(rule)
        );
        assertTrue(kb.findGroundings(rule).isEmpty());

        Predicate counter1 = new Predicate(TABLE_MALE, ARITY_MALE);
        counter1.args[0] = new Constant(CONSTANT_ID, "amie");
        Set<Predicate> counter_example_set = new HashSet<>();
        counter_example_set.add(counter1);
        assertEquals(counter_example_set, new HashSet<>(kb.findCounterExamples(rule)));
    }

    @Test
    @Order(10)
    void testFamily4() throws Exception {
        Rule rule = new Rule(TABLE_MALE, ARITY_MALE);
        rule.addPred(TABLE_FATHER, ARITY_FATHER);
        rule.boundFreeVars2NewVar(0, 0, 1, 0);
        rule.addPred(TABLE_FATHER, ARITY_FATHER);
        rule.boundFreeVars2NewVar(1, 1, 2, 0);
        assertEquals(
                String.format("(null)%s(X0):-%s(X0,X1),%s(X1,?)",
                        TABLE_MALE, TABLE_FATHER, TABLE_FATHER
                ), rule.toString()
        );
        String sql4all = getSql4AllEntailments(rule);
        String sql4new_pos = getSql4UnprovedPosEntailments(rule);
        Eval eval = kb.evalRule(rule);

        assertEquals(
                String.format(
                        "SELECT DISTINCT %s1.C0 AS X0 " +
                                "FROM %s AS %s1,%s AS %s2 " +
                                "WHERE %s1.C1=%s2.C0",
                        TABLE_FATHER,
                        TABLE_FATHER, TABLE_FATHER, TABLE_FATHER, TABLE_FATHER,
                        TABLE_FATHER, TABLE_FATHER
                ), sql4all
        );
        assertEquals(
                String.format(
                        "SELECT DISTINCT %s0.C0 AS C0 " +
                                "FROM %s AS %s0,%s AS %s1,%s AS %s2 " +
                                "WHERE %s0.C0=%s1.C0 AND %s1.C1=%s2.C0 " +
                                "EXCEPT SELECT * FROM %s",
                        TABLE_MALE,
                        TABLE_MALE, TABLE_MALE, TABLE_FATHER, TABLE_FATHER, TABLE_FATHER, TABLE_FATHER,
                        TABLE_MALE, TABLE_FATHER, TABLE_FATHER, TABLE_FATHER,
                        TABLE_MALE_PROVED
                ), sql4new_pos
        );
        assertEquals(new Eval(1, 1, 2), eval);

        Predicate predicate1 = new Predicate(TABLE_MALE, ARITY_MALE);
        predicate1.args[0] = new Constant(CONSTANT_ID, "tom");
        Set<Predicate> new_pos_preds = new HashSet<>();
        new_pos_preds.add(predicate1);
        assertEquals(new_pos_preds, query4Predicates(sql4new_pos, TABLE_MALE, ARITY_MALE));

        assertEquals(
                String.format(
                        "SELECT DISTINCT %s0.C0 AS X0,%s1.C1 AS X1,%s2.C1 AS X2 " +
                                "FROM %s AS %s0,%s AS %s1,%s AS %s2 " +
                                "WHERE %s0.C0=%s1.C0 AND %s1.C1=%s2.C0 " +
                                "GROUP BY X0",
                        TABLE_MALE, TABLE_FATHER, TABLE_FATHER,
                        TABLE_MALE, TABLE_MALE, TABLE_FATHER, TABLE_FATHER, TABLE_FATHER, TABLE_FATHER,
                        TABLE_MALE, TABLE_FATHER, TABLE_FATHER, TABLE_FATHER
                ), getSql4Groundings(rule)
        );
        Predicate body_pred1 = new Predicate(TABLE_FATHER, ARITY_FATHER);
        body_pred1.args[0] = new Constant(CONSTANT_ID, "tom");
        body_pred1.args[1] = new Constant(CONSTANT_ID, "jerry");
        Predicate body_pred2 = new Predicate(TABLE_FATHER, ARITY_FATHER);
        body_pred2.args[0] = new Constant(CONSTANT_ID, "jerry");
        body_pred2.args[1] = new Constant(CONSTANT_ID, "laura");

        Set<Predicate[]> grounding_set = new HashSet<>();
        grounding_set.add(new Predicate[]{predicate1, body_pred1, body_pred2});
        assertEqualOfArraySets(grounding_set, new HashSet<>(kb.findGroundings(rule)));

        assertTrue(kb.findCounterExamples(rule).isEmpty());
    }

    @Test
    @Order(11)
    void testFamily5() throws Exception {
        Rule rule = new Rule(TABLE_FATHER, ARITY_FATHER);
        rule.addPred(TABLE_MOTHER, ARITY_MOTHER);
        rule.boundFreeVars2NewVar(0, 1, 1, 1);
        assertEquals(
                String.format("(null)%s(?,X0):-%s(?,X0)",
                        TABLE_FATHER, TABLE_MOTHER
                ), rule.toString()
        );
        String sql4all = getSql4AllEntailments(rule);
        String sql4new_pos = getSql4UnprovedPosEntailments(rule);
        Eval eval = kb.evalRule(rule);

        assertEquals(
                String.format(
                        "SELECT DISTINCT %s1.C1 AS X0 " +
                                "FROM %s AS %s1 ",
                        TABLE_MOTHER,
                        TABLE_MOTHER, TABLE_MOTHER
                ), sql4all
        );
        assertEquals(
                String.format(
                        "SELECT DISTINCT %s0.C0 AS C0,%s0.C1 AS C1 " +
                                "FROM %s AS %s0,%s AS %s1 " +
                                "WHERE %s0.C1=%s1.C1 " +
                                "EXCEPT SELECT * FROM %s",
                        TABLE_FATHER, TABLE_FATHER,
                        TABLE_FATHER, TABLE_FATHER, TABLE_MOTHER, TABLE_MOTHER,
                        TABLE_FATHER, TABLE_MOTHER,
                        TABLE_FATHER_PROVED
                ), sql4new_pos
        );
        assertEquals(new Eval(1, 56, 1), eval);

        Predicate predicate1 = new Predicate(TABLE_FATHER, ARITY_FATHER);
        predicate1.args[0] = new Constant(CONSTANT_ID, "jerry");
        predicate1.args[1] = new Constant(CONSTANT_ID, "laura");
        Set<Predicate> new_pos_preds = new HashSet<>();
        new_pos_preds.add(predicate1);
        assertEquals(new_pos_preds, query4Predicates(sql4new_pos, TABLE_FATHER, ARITY_FATHER));

        assertEquals(
                String.format(
                        "SELECT DISTINCT %s0.C0 AS X1,%s0.C1 AS X0,%s1.C0 AS X2 " +
                                "FROM %s AS %s0,%s AS %s1 " +
                                "WHERE %s0.C1=%s1.C1 " +
                                "GROUP BY X1,X0",
                        TABLE_FATHER, TABLE_FATHER, TABLE_MOTHER,
                        TABLE_FATHER, TABLE_FATHER, TABLE_MOTHER, TABLE_MOTHER,
                        TABLE_FATHER, TABLE_MOTHER
                ), getSql4Groundings(rule)
        );
        Predicate body_pred1 = new Predicate(TABLE_MOTHER, ARITY_MOTHER);
        body_pred1.args[0] = new Constant(CONSTANT_ID, "amie");
        body_pred1.args[1] = new Constant(CONSTANT_ID, "laura");

        Set<Predicate[]> grounding_set = new HashSet<>();
        grounding_set.add(new Predicate[]{predicate1, body_pred1});
        assertEqualOfArraySets(grounding_set, new HashSet<>(kb.findGroundings(rule)));

        assertEquals(55, kb.findCounterExamples(rule).size());
    }

    @Test
    @Order(12)
    void testRoad1() throws Exception {
        Rule rule = new Rule(TABLE_ROAD, ARITY_ROAD);
        rule.boundFreeVars2NewVar(0, 1, 0, 3);
        rule.boundFreeVars2NewVar(0, 2, 0, 4);
        assertEquals(String.format("(null)%s(?,X0,X1,X0,X1):-", TABLE_ROAD), rule.toString());
        String sql4all = getSql4AllEntailments(rule);
        String sql4new_pos = getSql4UnprovedPosEntailments(rule);
        Eval eval = kb.evalRule(rule);

        assertNull(sql4all);
        assertEquals(
                String.format(
                        "SELECT DISTINCT %s0.C0 AS C0,%s0.C1 AS C1,%s0.C2 AS C2,%s0.C3 AS C3,%s0.C4 AS C4 " +
                                "FROM %s AS %s0 " +
                                "WHERE %s0.C1=%s0.C3 AND %s0.C2=%s0.C4 " +
                                "EXCEPT SELECT * FROM %s",
                        TABLE_ROAD, TABLE_ROAD, TABLE_ROAD, TABLE_ROAD, TABLE_ROAD,
                        TABLE_ROAD, TABLE_ROAD,
                        TABLE_ROAD, TABLE_ROAD, TABLE_ROAD, TABLE_ROAD,
                        TABLE_ROAD_PROVED
                ), sql4new_pos
        );
        assertEquals(new Eval(1, 21952, 2), eval);

        Predicate predicate1 = new Predicate(TABLE_ROAD, ARITY_ROAD);
        predicate1.args[0] = new Constant(CONSTANT_ID, "上海外环");
        predicate1.args[1] = new Constant(CONSTANT_ID, "上海");
        predicate1.args[2] = new Constant(CONSTANT_ID, "南");
        predicate1.args[3] = new Constant(CONSTANT_ID, "上海");
        predicate1.args[4] = new Constant(CONSTANT_ID, "南");
        Set<Predicate> new_pos_preds = new HashSet<>();
        new_pos_preds.add(predicate1);
        assertEquals(new_pos_preds, query4Predicates(sql4new_pos, TABLE_ROAD, ARITY_ROAD));

        assertEquals(
                String.format(
                        "SELECT DISTINCT %s0.C0 AS X2,%s0.C1 AS X0,%s0.C2 AS X1 " +
                                "FROM %s AS %s0 " +
                                "WHERE %s0.C1=%s0.C3 AND %s0.C2=%s0.C4 " +
                                "GROUP BY X2,X0,X1",
                        TABLE_ROAD, TABLE_ROAD, TABLE_ROAD,
                        TABLE_ROAD, TABLE_ROAD,
                        TABLE_ROAD, TABLE_ROAD, TABLE_ROAD, TABLE_ROAD
                ), getSql4Groundings(rule)
        );

        Set<Predicate[]> grounding_set = new HashSet<>();
        grounding_set.add(new Predicate[]{predicate1});
        assertEqualOfArraySets(grounding_set, new HashSet<>(kb.findGroundings(rule)));

        assertEquals(21951, kb.findCounterExamples(rule).size());
    }

    @Test
    @Order(13)
    void testRoad2() throws Exception {
        Rule rule = new Rule(TABLE_ROAD, ARITY_ROAD);
        rule.boundFreeVars2NewVar(0, 1, 0, 3);
        rule.boundFreeVars2NewVar(0, 2, 0, 4);
        rule.addPred(TABLE_ROAD_TYPE, ARITY_ROAD_TYPE);
        rule.boundFreeVars2NewVar(0, 0, 1, 0);
        rule.boundFreeVar2Constant(1, 2, CONSTANT_ID, "环");
        assertEquals(
                String.format(
                        "(null)%s(X2,X0,X1,X0,X1):-%s(X2,?,环)", TABLE_ROAD, TABLE_ROAD_TYPE
                ), rule.toString()
        );
        String sql4all = getSql4AllEntailments(rule);
        String sql4new_pos = getSql4UnprovedPosEntailments(rule);
        Eval eval = kb.evalRule(rule);

        assertEquals(
                String.format(
                        "SELECT DISTINCT %s1.C0 AS X2 " +
                                "FROM %s AS %s1 " +
                                "WHERE %s1.C2='环'",
                        TABLE_ROAD_TYPE,
                        TABLE_ROAD_TYPE, TABLE_ROAD_TYPE,
                        TABLE_ROAD_TYPE
                ), sql4all
        );
        assertEquals(
                String.format(
                        "SELECT DISTINCT %s0.C0 AS C0,%s0.C1 AS C1,%s0.C2 AS C2,%s0.C3 AS C3,%s0.C4 AS C4 " +
                                "FROM %s AS %s0,%s AS %s1 " +
                                "WHERE %s0.C1=%s0.C3 AND %s0.C2=%s0.C4 AND %s0.C0=%s1.C0 AND %s1.C2='环' " +
                                "EXCEPT SELECT * FROM %s",
                        TABLE_ROAD, TABLE_ROAD, TABLE_ROAD, TABLE_ROAD, TABLE_ROAD,
                        TABLE_ROAD, TABLE_ROAD, TABLE_ROAD_TYPE, TABLE_ROAD_TYPE,
                        TABLE_ROAD, TABLE_ROAD, TABLE_ROAD, TABLE_ROAD, TABLE_ROAD, TABLE_ROAD_TYPE, TABLE_ROAD_TYPE,
                        TABLE_ROAD_PROVED
                ), sql4new_pos
        );
        assertEquals(new Eval(1, 1568, 4), eval);

        Predicate predicate1 = new Predicate(TABLE_ROAD, ARITY_ROAD);
        predicate1.args[0] = new Constant(CONSTANT_ID, "上海外环");
        predicate1.args[1] = new Constant(CONSTANT_ID, "上海");
        predicate1.args[2] = new Constant(CONSTANT_ID, "南");
        predicate1.args[3] = new Constant(CONSTANT_ID, "上海");
        predicate1.args[4] = new Constant(CONSTANT_ID, "南");
        Set<Predicate> new_pos_preds = new HashSet<>();
        new_pos_preds.add(predicate1);
        assertEquals(new_pos_preds, query4Predicates(sql4new_pos, TABLE_ROAD, ARITY_ROAD));

        assertEquals(
                String.format(
                        "SELECT DISTINCT %s0.C0 AS X2,%s0.C1 AS X0,%s0.C2 AS X1,%s1.C1 AS X3 " +
                                "FROM %s AS %s0,%s AS %s1 " +
                                "WHERE %s0.C1=%s0.C3 AND %s0.C2=%s0.C4 AND %s0.C0=%s1.C0 AND %s1.C2='环' " +
                                "GROUP BY X2,X0,X1",
                        TABLE_ROAD, TABLE_ROAD, TABLE_ROAD, TABLE_ROAD_TYPE,
                        TABLE_ROAD, TABLE_ROAD, TABLE_ROAD_TYPE, TABLE_ROAD_TYPE,
                        TABLE_ROAD, TABLE_ROAD, TABLE_ROAD, TABLE_ROAD, TABLE_ROAD, TABLE_ROAD_TYPE, TABLE_ROAD_TYPE
                ), getSql4Groundings(rule)
        );

        Predicate body_pred1 = new Predicate(TABLE_ROAD_TYPE, ARITY_ROAD_TYPE);
        body_pred1.args[0] = new Constant(CONSTANT_ID, "上海外环");
        body_pred1.args[1] = new Constant(CONSTANT_ID, "公路");
        body_pred1.args[2] = new Constant(CONSTANT_ID, "环");

        Set<Predicate[]> grounding_set = new HashSet<>();
        grounding_set.add(new Predicate[]{predicate1, body_pred1});
        assertEqualOfArraySets(grounding_set, new HashSet<>(kb.findGroundings(rule)));

        assertEquals(1567, kb.findCounterExamples(rule).size());
    }

    @Test
    @Order(14)
    void testUpdateProof() throws Exception {
        Rule rule = new Rule(TABLE_CONNECTED, ARITY_CONNECTED);
        rule.addPred(TABLE_LINKED, ARITY_LINKED);
        rule.boundFreeVars2NewVar(0, 0, 1, 0);
        rule.addPred(TABLE_LINKED, ARITY_LINKED);
        rule.boundFreeVars2NewVar(0, 1, 2, 1);
        rule.boundFreeVars2NewVar(1, 1, 2, 0);
        assertEquals(
                String.format("(null)%s(X0,X1):-%s(X0,X2),%s(X2,X1)",
                        TABLE_CONNECTED, TABLE_LINKED, TABLE_LINKED
                ), rule.toString()
        );
        List<Predicate[]> groudings = kb.findGroundings(rule);
        assertEquals(3, groudings.size());
        kb.addNewProofs(groudings);

        Predicate connected1 = new Predicate(TABLE_CONNECTED_PROVED, ARITY_CONNECTED);
        connected1.args[0] = new Constant(CONSTANT_ID, "a");
        connected1.args[1] = new Constant(CONSTANT_ID, "b");
        Predicate connected2 = new Predicate(TABLE_CONNECTED_PROVED, ARITY_CONNECTED);
        connected2.args[0] = new Constant(CONSTANT_ID, "a");
        connected2.args[1] = new Constant(CONSTANT_ID, "d");
        Predicate connected3 = new Predicate(TABLE_CONNECTED_PROVED, ARITY_CONNECTED);
        connected3.args[0] = new Constant(CONSTANT_ID, "f");
        connected3.args[1] = new Constant(CONSTANT_ID, "d");
        Predicate connected4 = new Predicate(TABLE_CONNECTED_PROVED, ARITY_CONNECTED);
        connected4.args[0] = new Constant(CONSTANT_ID, "a");
        connected4.args[1] = new Constant(CONSTANT_ID, "c");
        Predicate connected5 = new Predicate(TABLE_CONNECTED_PROVED, ARITY_CONNECTED);
        connected5.args[0] = new Constant(CONSTANT_ID, "e");
        connected5.args[1] = new Constant(CONSTANT_ID, "b");
        Predicate connected6 = new Predicate(TABLE_CONNECTED_PROVED, ARITY_CONNECTED);
        connected6.args[0] = new Constant(CONSTANT_ID, "e");
        connected6.args[1] = new Constant(CONSTANT_ID, "d");
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
        assertEquals(connected_set, kb.listPredicate(TABLE_CONNECTED_PROVED, ARITY_CONNECTED));

        Rule rule2 = new Rule(TABLE_CONNECTED, ARITY_CONNECTED);
        rule2.boundFreeVar2Constant(0, 0, CONSTANT_ID, "e");
        assertEquals(String.format("(null)%s(e,?):-", TABLE_CONNECTED), rule2.toString());
        Eval eval = kb.evalRule(rule2);
        assertEquals(new Eval(2, 28, 1), eval);

        Predicate predicate1 = new Predicate(TABLE_CONNECTED, ARITY_CONNECTED);
        predicate1.args[0] = new Constant(CONSTANT_ID, "e");
        predicate1.args[1] = new Constant(CONSTANT_ID, "a");
        Predicate predicate2 = new Predicate(TABLE_CONNECTED, ARITY_CONNECTED);
        predicate2.args[0] = new Constant(CONSTANT_ID, "e");
        predicate2.args[1] = new Constant(CONSTANT_ID, "c");
        Set<Predicate> new_pos_preds = new HashSet<>();
        new_pos_preds.add(predicate1);
        new_pos_preds.add(predicate2);
        assertEquals(
                new_pos_preds,
                query4Predicates(
                        getSql4UnprovedPosEntailments(rule2), TABLE_CONNECTED, ARITY_CONNECTED
                )
        );
    }

    private String getSql4AllEntailments(Rule rule) throws Exception {
        Method mtd = RKB.class.getDeclaredMethod("parseSql4AllEntailments", Rule.class);
        mtd.setAccessible(true);
        return (String) mtd.invoke(kb, rule);
    }

    private String getSql4UnprovedPosEntailments(Rule rule) throws Exception {
        Method mtd = RKB.class.getDeclaredMethod("parseSql4UnprovedPosEntailments", Rule.class);
        mtd.setAccessible(true);
        return (String) mtd.invoke(kb, rule);
    }

    private String getSql4Groundings(Rule rule) throws Exception {
        Method mtd = RKB.class.getDeclaredMethod("parseSql4RuleGroundings", Rule.class);
        mtd.setAccessible(true);
        return (String) mtd.invoke(kb, rule);
    }

    private Set<Predicate> query4Predicates(String sql, String functor, int arity) throws Exception {
        Statement statement = connection.createStatement();
        ResultSet result_set = statement.executeQuery(sql);
        Set<Predicate> result = new HashSet<>();
        while (result_set.next()) {
            Predicate predicate = new Predicate(functor, arity);
            for (int i = 0; i < arity; i++) {
                predicate.args[i] = new Constant(CONSTANT_ID, result_set.getString("C" + i));
            }
            result.add(predicate);
        }
        return result;
    }

    <T> void assertEqualOfArraySets(Set<T[]> expected, Set<T[]> actual) {
        assertEquals(expected.size(), actual.size());
        for (T[] expected_arr: expected) {
            boolean found = false;
            for (T[] actual_arr: actual) {
                if (Arrays.equals(expected_arr, actual_arr)) {
                    found = true;
                    break;
                }
            }
            assertTrue(found);
        }
    }
}