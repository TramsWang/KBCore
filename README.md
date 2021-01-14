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

