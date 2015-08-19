# This script is called, when the configured modules are already exported from a JSP in OpenCms
# It is called as
# ./modules-chekin.sh <config-file> [modules]
# if no modules are given as second parameter, the module list from the config-file are taken

## Error codes
# 1 - pull failed
# 2 - push failed
# 3 - no config file provided
# 4 - something went wrong when changing folders
# 5 - the configured repository main folder does not exist
# 6 - the configured repository main folder is not a repository main folder
# 7 - the module export folder does not exist
# 8 - the repository's module main folder does not exist

getExportedModule(){
	case $exportMode in
		1 ) echo $( ls -t | grep "${module}[\.\-\_]\([0-9]\{1,4\}\.\)\{0,3\}zip" | head -n1 )
			;;
		* ) echo $( ls | grep "${module}.zip" | head -n1 )
	esac
}

testGitRepository(){
	__pwd=$(pwd)
	cd $REPOSITORY_HOME
	if [[ $? != 0 ]]; then
		echo "ERROR: The GIT repository's main folder \"$REPOSITORY_HOME\" does not exist."
		exit 5
	fi
	git status >/dev/null
	if [[ $? != 0 ]]; then
		echo "ERROR: You have not specified a GIT repository's main folder via\
              \"REPOSITORY_HOME\" ($REPOSITORY_HOME)"
		exit 6
	fi
	echo " * Test ok: found git repository at \"$(pwd)\"."
	cd $__pwd
}

testModuleExportFolder(){
	__pwd=$(pwd)
	cd $moduleExportFolder
	if [[ $? != 0 ]]; then
		echo "ERROR: The specified module export folder \"$moduleExportFolder\" does not exist."
		exit 7
	fi
	echo " * Test ok: Module export folder \"$(pwd)\" exists."
	cd $_pwd
}

testModuleMainFolder(){
	__pwd=$(pwd)
	cd $MODULE_PATH
	if [[ $? != 0 ]]; then
		echo "ERROR: The specified module export folder \"$MODULE_PATH\" does not exist."
		exit 8
	fi
	echo " * Test ok: Module main folder \"$(pwd)\" exists."
	cd $__pwd
}


echo
echo "Started script for automatic check in of OpenCms modules into a GIT repository."
echo "-------------------------------------------------------------------------------"
echo
echo "Reading command line arguments ..."

#read commandline arguments
while [ "$1" != "" ]; do
    case $1 in
        -m | --modules )       	shift
                                modulesToExport=$1
								echo " * Read modules to export: \"$modulesToExport\"." 
                                ;;
        --push )                push=1
								echo " * Read push option."
                                ;;
        --no-push )             push=0
								echo " * Read no-push option."
                                ;;
        --pull )                pull=1
								echo " * Read pull option."
                                ;;
        --no-pull )             pull=0
								echo " * Read no-pull option."
                                ;;
        --commit )              commit=1
								echo " * Read commit option."
                                ;;
        --no-commit )           commit=0
								echo " * Read no-commit option."
                                ;;                                
        --no-exclude-libs )		excludeLibs=0;
        						echo " * Read no-exclude-libs option."
        						;;
        --exclude-libs )		excludeLibs=1;
        						echo " * Read exclude-libs option."
        						;;
        -msg )					shift
			        			commitMessage=$1
								echo " * Read commit message: \"$commitMessage\"."
								;;
		--export-folder )		shift
								moduleExportFolder=$1
								echo " * Read module export folder: \"$moduleExportFolder\"."
								;;
		--export-mode )			shift
								exportMode=$1
								echo " * Read export mode: $exportMode."
								;;
        * )                     configfile=$1
								echo " * Read config file: \"$configfile\"."
    esac
    shift
done

echo
echo "Reading configuration file ..."
if [[ -z "$configfile" ]]; then
	echo " * ERROR: No config file provided."
	exit 3;

fi
source $configfile
echo " * Read file \"$configfile\":"
cat $configfile | awk '$0="   * "$0'

echo
echo "Setting parameters ..."
## set push flag
if [[ -z "$push" ]]; then
	if [[ -z "$GIT_PUSH" ]]; then
		$push=0
		echo " * Git push mode not specified. Using mode 0, i.e. do not push."
	else
		push=$GIT_PUSH
	fi	
fi
echo " * Set auto-push: $push."

## set pull flag
if [[ -z "$pull" ]]; then
	if [[ -z "$GIT_PULL" ]]; then
		pull=0
		echo " * Git pull mode not specified. Using mode 0, i.e. do not pull."
	else
		pull=$GIT_PULL
	fi	
fi
echo " * Set auto-pull: $pull."

## set commit flag
if [[ -z "$commit" ]]; then
	if [[ -z "$GIT_COMMIT" ]]; then
		commit=0
		echo " * Git commit mode not specified. Using mode 0, i.e. do not commit."
	else
		commit=$GIT_COMMIT
	fi	
fi
echo " * Set auto-commit: $commit."

## set modules to export
if [[ -z "$modulesToExport" ]]; then
	modulesToExport=$DEFAULT_MODULES_TO_EXPORT
fi
echo " * Set modules to export: \"$modulesToExport\"."

## set export mode
if [[ -z "$exportMode" ]]; then
	exportMode=$MODULE_EXPORT_MODE
fi
case $exportMode in
	1 )	;;
	* ) exportMode=0
esac
echo " * Set export mode: $exportMode."

## set commit message
if [[ -z "$commitMessage" ]]; then
	if [[ -z "$COMMIT_MESSAGE" ]]; then
		commitMessage="Autocommit of exported modules."
	else
		commitMessage="$COMMIT_MESSAGE"
	fi
fi
echo " * Set commit message: \"$commitMessage\"."

## set module export folder
if [[ -z "$moduleExportFolder" ]]; then
	moduleExportFolder=$MODULE_EXPORT_FOLDER
fi
echo " * Set module export folder: \"$moduleExportFolder\"."

## set export libs flag
if [[ -z "$excludeLibs" ]]; then
	if [[ -z "$COMMIT_MESSAGE" ]]; then
		excludeLibs=0
	else
		excludeLibs=$DEFAULT_EXCLUDE_LIBS
	fi
fi
echo " * Set exclude libs flag: $excludeLibs."


## test if all necessary options are set
echo
echo "Testing folders ..."
testGitRepository
testModuleExportFolder
testModuleMainFolder

echo
echo "Performing pull ..."
## prepare the repository by pulling if wanted
if [[ $pull == 1 ]]; then
	cd $REPOSITORY_HOME
	if [[ ! -z "$GIT_SSH" ]]; then
		echo "  * Pulling with specified ssh keys."
		ssh-agent bash -c "ssh-add $GIT_SSH; git pull"
	else
		echo "  * Pulling."
		git pull
	fi
	pullExitCode=$?
	if [[ $pullExitCode != 0 ]]; then
		echo "   * ERROR: Pull failed: $pullExitCode."
		exit 1
	fi
else
	echo " * Skip pulling."
fi
echo

echo "Copy and unzip modules ..."
## copy and unzip modules
for module in $modulesToExport; do
	echo
	echo " * Handling module ${module} ..."
	echo
	cd $moduleExportFolder
	fileName=$(getExportedModule)
	if [[ ! -z "$fileName" ]]; then
		echo "   * Found zip file ${fileName}."
		#switch to project's module path
		cd "${MODULE_PATH}"
		#check if a subdirectory for the module exists - if not add it
		if [ ! -d "$module" ]; then
			echo "   * Creating missing module directory \"$module\" under \"$(pwd)\"."
			mkdir $module
		fi
		#go to the modules' subfolder in the project
		cd $module
		#if necessary, add the resources' subfolder of the module
		if [[ (! -z "$MODULE_RESOURCES_SUBFOLDER") && (! -d "$MODULE_RESOURCES_SUBFOLDER") ]]; then
			echo "   * Creating missing resources subfolder \"$MODULE_RESOURCES_SUBFOLDER\"\
				       under $(pwd)."
			mkdir $MODULE_RESOURCES_SUBFOLDER
		fi
		#if there's a resources subfolder, switch to it
		if [[ -d "$MODULE_RESOURCES_SUBFOLDER" ]]; then
			cd $MODULE_RESOURCES_SUBFOLDER
		fi
		#delete all resources currently checked in in the project
		if [[ "$(pwd)" == "${MODULE_PATH}"* ]]; then
			echo "   * Removing old version of the module resources under $(pwd)."
			rm -fr ./*
		else
			echo "   * ERROR: Something went wrong the current directory ($(pwd)) is not a\
				  subdirectory of the repository's configured modules main folder (${MODULE_PATH})." 
			exit 4
		fi			
		echo "   * Copying "${moduleExportFolder}/${fileName}" to $(pwd) ..."
		#copy the new module .zip
		cp "${moduleExportFolder}/${fileName}" ./
		echo "   * Unzipping copied file."
		#unzip it
		unzip -o "${fileName}" | awk '$0="     "$0'
		echo "   * Deleting copy of the .zip file."
		#remove the .zip file
		rm "${fileName}"
		#remove lib/ subfolder if necessary
		echo "   * Removing lib folder ..."
		if [[ $excludeLibs == 1 ]]; then
			libFolder="system/modules/${module}/lib"
			if [[ -d "$libFolder" ]]; then
				rm -fr "$libFolder"
				echo "     * ... lib/ folder \"$(pwd)/$libFolder\" removed."
			else
				echo "     * ... lib/ folder \"$(pwd)/$libFolder\" does not exist. Do nothing."
						fi
		else
			echo "     * ... lib folder shall not be removed. Do nothing."
		fi
	else
		echo "   ! WARN: Skipped module $module because the zip file was not found."
	fi
done

echo
echo "Performing commit ..."
# commit changes
cd $REPOSITORY_HOME
if [[ $commit == 1 ]]; then
	echo " * Check in to GIT repository"
	echo "   * Step 1: git add $MODULE_PATH/*"
	git add $MODULE_PATH/* | awk '$0="   "$0'
	echo "   * Step 2: bash -c \"git commit -m \\\"$commitMessage\\\" \""
	bash -c "git commit -m \"$commitMessage\"" | awk '$0="     "$0'
else
	echo " * Auto-commit disabled. Nothing to do."
fi

echo 
echo "Pushing changes ..."
# pushing changes
if [[ $push == 1 ]]; then
		if [[ ! -z "$GIT_SSH" ]]; then
		echo
		echo " * Pushing changes using configured SSH keys."
		pushExitCode=$(ssh-agent bash -c "ssh-add $GIT_SSH; git push & echo $?")
	else
		echo
		echo " * Pushing changes."
		git push
		pushExitCode=$?
	fi
	if [[ $pushExitCode != 0 ]]; then
		echo "   * WARN: Pushing failed: $pushExitCode."
		exit 2
	fi
else
	echo ' * Auto-Push is disabled. Do nothing.'
fi

echo
echo "Script completed successfully."
echo "------------------------------"
echo

exit 0