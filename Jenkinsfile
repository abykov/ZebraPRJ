pipeline {
    agent any

    stages {
        // ====================== NEW DIAGNOSTIC STAGE ======================
        stage('Discover Environment') {
            steps {
                sh '''
                    echo "!!!!!!!!!! STARTING ENVIRONMENT DISCOVERY !!!!!!!!!!"

                    echo "--- Step 1: Finding the 'java' executable ---"
                    WHICH_JAVA=$(which java)
                    echo "Command 'which java' found it at: ${WHICH_JAVA}"
                    echo ""

                    echo "--- Step 2: Resolving the real path (following symbolic links) ---"
                    # The 'readlink -f' command gives the true, canonical path
                    REAL_JAVA_PATH=$(readlink -f ${WHICH_JAVA})
                    echo "The real path to the java binary is: ${REAL_JAVA_PATH}"
                    echo ""

                    echo "--- Step 3: Determining the correct JAVA_HOME ---"
                    # The JAVA_HOME is usually two directories above the 'bin/java' file
                    CORRECT_JAVA_HOME=$(dirname $(dirname ${REAL_JAVA_PATH}))
                    echo "THE CORRECT JAVA_HOME FOR THIS CONTAINER IS: ${CORRECT_JAVA_HOME}"
                    echo ""

                    echo "!!!!!!!!!! DISCOVERY COMPLETE !!!!!!!!!!"
                '''
            }
        }

        // We will skip all other stages to get our answer quickly.
        stage('Other') {
            steps {
                echo "Skipping further stages during diagnostic."
            }
            when {
                expression { false }
            }
        }
    }
}