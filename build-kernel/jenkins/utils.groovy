/*
 * SPDX-License-Identifier: EUPL-1.2 OR LicenseRef-commercial
 *
 * Copyright (c) 2012-2026 mgm technology partners GmbH
 *
 * Dual License
 * ------------
 * This source file is part of the mgm A12 Platform and available under
 * a choice of two different licenses:
 *
 * 1. Open-Source License – EUPL v1.2
 *    You may redistribute and/or modify this file under the terms of the
 *    European Union Public License, version 1.2 - see https://eupl.eu/.
 *
 * 2. Commercial License
 *    Alternatively, you may obtain a commercial license from
 *    mgm technology partners GmbH, that permits use of this software
 *    under different terms (including support and maintenance services).
 *
 *    Please contact a12-license@mgm-tp.com for more information.
 *
 * You must select and comply with exactly one of the above license options.
 *
 * Warranty Disclaimer (applies to either option)
 * ----------------------------------------------
 * THIS SOFTWARE IS PROVIDED “AS IS” AND WITHOUT WARRANTY OF ANY KIND,
 * WHETHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT, EXCEPT WHERE SUCH DISCLAIMERS ARE HELD TO BE
 * LEGALLY INVALID. SEE THE RESPECTIVE LICENSE TEXT FOR DETAILS.
 */
// credentials and env
void gradleCredentials(final Closure body) {
    withCredentials([usernamePassword(credentialsId: 'ci-user-artifactory-token', usernameVariable: 'repository_username', passwordVariable: 'repository_password')]) {
        configFileProvider([configFile(fileId: 'gradle-settings', targetLocation: "${GRADLE_USER_HOME}/init.d/repositories.gradle")]) {
            body()
        }
    }
}

void bitbucketCredentials(final Closure body) {
    withCredentials([usernamePassword(credentialsId: 'ci-user-bitbucket-token', usernameVariable: 'bitbucket_username', passwordVariable: 'bitbucket_password')]) {
        body()
    }
}

void npmCredentials(final Closure body) {
    configFileProvider([configFile(fileId: 'npmrc', targetLocation: "${WORKSPACE}/jenkins.npmrc")]) {
        body()
    }
}

void atlasCredentials(final Closure body) {
    withCredentials([usernamePassword(credentialsId: 'atlas-a12-ci', usernameVariable: 'ATLAS_USERNAME', passwordVariable: 'ATLAS_PASSWORD')]) {
        body()
    }
}

void sshAgent(final Closure body) {
    sshagent(credentials: ['ci-user-ssh']) {
        withCredentials([sshUserPrivateKey(credentialsId: 'ci-user-ssh', usernameVariable: 'ssh_username', keyFileVariable: 'KEYFILE_NOT_NEEDED')]) {
            body()
        }
    }
}

void sonarQube(final Closure body) {
    withEnv(["SONAR_USER_HOME=${env.WORKSPACE}"]) {
        withCredentials([string(credentialsId: 'ci-user-sonarqube-token', variable: 'sonar_token')]) {
            body()
        }
    }
}

void buildEnvironmentWithoutSSH(final Closure body) {
    bitbucketCredentials {
        gradleCredentials {
            npmCredentials {
                body()
            }
        }
    }
}

void buildEnvironment(final Closure body) {
    buildEnvironmentWithoutSSH {
        sshAgent {
            body()
        }
    }
}

// general initialization
void init() {
    initEnv()
}

void initEnv() {
    env.GRADLE_USER_HOME = "${env.WORKSPACE}/.gradle"
    env.NPM_CONFIG_USERCONFIG = "${env.WORKSPACE}/jenkins.npmrc"
    env.NPM_CONFIG_CACHE = "${env.WORKSPACE}/npm2/cache"
    env.CI = true

    env.TZ = 'America/Los_Angeles'
    env.LC_ALL = 'cs_CZ.UTF-8'
}

// git and build
String branchName() {
    if (env.CHANGE_BRANCH != null && env.CHANGE_BRANCH.isEmpty()) {
        env.CHANGE_BRANCH
    } else {
        env.GIT_BRANCH
    }
}

// gradle
String publishTaskName() {
    'publishAllPublicationsToMgm-maven-releasesRepository'
}

return this