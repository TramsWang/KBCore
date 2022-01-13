# KBCore
Mining essential knowledge in knowledge bases

## 0. Prerequisits

### 0.1. SWIPL & JPL

#### 0.1.1. Download & Install SWIPL

Download from official git repo:

```sh
git clone https://github.com/SWI-Prolog/swipl
```

Install SWIPL from source: Details referred to this link​ https://www.swi-prolog.org/build/ 

#### 0.1.2. Set Environment Vars in Running Environment

```sh
SWI_HOME_DIR=/usr/lib/swi-prolog/   # if default binary not pointing to this version
LD_LIBRARY_PATH=/usr/lib/swi-prolog/lib/x86_64-linux/   # to find all .so, including libjpl.so
CLASSPATH=/usr/lib/swi-prolog/lib/jpl.jar # Java API
LD_PRELOAD=/usr/lib/libswipl.so  # core SWIPL
```

Installation will usually match the above location. If not, find the corresponding ones in the system(maybe in your home dir).

### 0.2. Data Format

IKnowS is implemented in Java and require version 11+. All of its dependencies are publicly accessible via Maven.

Input data format is `.tsv` files. Each line of the file is the following format:

```
<relation>\t<arg1>\t<arg2>\t...<argn>
```

For example:

```
President	USA	Trump
Family	ginny	harry	albus
```

## 2. Usage

The basic class for summarization is the abstract class `IKnowS`. To sumamrize relational data, simply create an IKnowS implementation object and invoke `run()` on it. For exmaple:

```java
IknowsConfig config = new IknowsConfig(1,false,false,5,true,eval_type,0.05,0.25,false,-1.0,false,false);
IKnowS iknows = new IknowsWithRecalculateCache(config,"elti.tsv",null,null);
iknows.run();
List<Rule> hypothesis = iknows.getHypothesis();  // H
Set<Predicate> necessary = iknows.getStartSet();  // N
Set<Predicate> counter_examples = iknows.getCounterExamples();  // C
```

Arguments in `IknowsConfig` are for debugging, experiments and possible extensions. Currently, the following arguments will take effect:

- `beamWidth`
- `evalMetric`
- `minFactCoverage`
- `minConstantCoverage`

Current implementation of `IKnowS` is `IknowsWithRecalculateCache`, other implementations are for experiments. Arguments to initialize a IKnowS object include:

- `config`: A `IknowsConfig` object
- `kbPath`: The path to the input data file
- `dumpPath`: The path to sumamrized data file. If null, output will go to the terminal
- `logPath`: The path to a log file. If null, output will go to the terminal