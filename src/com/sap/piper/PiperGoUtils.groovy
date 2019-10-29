package com.sap.piper

class PiperGoUtils implements Serializable {

    private static Script steps
    private static Utils utils

    private static String DELIMITER = '-DeLiMiTeR-'

    PiperGoUtils(Script steps) {
        this.steps = steps
        this.utils = new Utils()
    }

    PiperGoUtils(Script steps, Utils utils) {
        this.steps = steps
        this.utils = utils
    }

    void unstashPiperBin() {

        if (utils.unstash('piper-bin').size() > 0) return

        def libraries = getLibrariesInfo()
        String version
        libraries.each {lib ->
            if (lib.name == 'piper-lib-os') {
                version = lib.version
            }
        }

        def fallbackUrl = 'https://github.com/SAP/jenkins-library/releases/latest/download/piper_master'
        def piperBinUrl = (version == 'master') ? fallbackUrl : "https://github.com/SAP/jenkins-library/releases/tag/${version}"

        boolean downloaded = downloadGoBinary(piperBinUrl)
        if (!downloaded) {
            //Inform that no Piper binary is available for used library branch
            steps.echo ("Not able to download go binary of Piper for version ${version}")
            //Fallback to master version & throw error in case this fails
            if (!downloadGoBinary(fallbackUrl)) steps.error("Download of Piper go binary failed.")
        }
        utils.stashWithMessage('piper-bin', 'failed to stash piper binary', 'piper')
    }

    List getLibrariesInfo() {
        return new JenkinsUtils().getLibrariesInfo()
    }

    private boolean downloadGoBinary(url) {

        def response = steps.sh(returnStdout: true, script: "curl --insecure --silent --location --write-out '${DELIMITER}status=%{http_code}' --output ./piper '${url}'")

        def parts = response.split(DELIMITER)
        if (parts.size() > 1 && parts[1] == 'status=200') {
            steps.sh(script: 'chmod +x ./piper')
            return true
        }
        return false
    }
}
