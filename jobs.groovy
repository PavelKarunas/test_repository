for (i=1; i<5; i++) {
	job("MNTLAB-pkarunas-child$i-build-job") {
	   	description('Child DSL job')
	    parameters {
	        activeChoiceParam('BRANCH_NAME') {
	            description('Select branch')
	            choiceType('SINGLE_SELECT')
	            groovyScript {
	                script('''
def gitURL = "https://github.com/MNT-Lab/dsl-task.git"
def command = "git ls-remote -h $gitURL"
def proc = command.execute()
def branches = proc.in.text.readLines().collect {
	it.replaceAll(/[a-z0-9]*\\trefs\\/heads\\//, '')
	}
return branches
	                ''')
	            }
	        }
	    }
		scm {
		    git('https://github.com/MNT-Lab/dsl-task.git', '$BRANCH_NAME', {node -> node / 'extensions' << '' })
		}
		steps {
		    shell ('''
chmod +x script.sh  
./script.sh > output.txt 
tar czvf ${BRANCH_NAME}_dsl_script.tar.gz output.txt 
				''')
		}
		publishers {
			archiveArtifacts{
				pattern('jobs.groovy')
				pattern('${BRANCH_NAME}_dsl_script.tar.gz')
				onlyIfSuccessful()
			}
		}
	}
}
job('MNTLAB-pkarunas-main-build-job') {
   	description('Parent DSL job')
    parameters {
        choiceParam('BRANCH_NAME', ['pkarunas', 'master'], 'Select branch')
        activeChoiceParam('BUILDS_TRIGGER') {
            description('Select jobs to run')
            choiceType('CHECKBOX')
            groovyScript {
                script('''
def list=[]
for (i=1; i<5; i++) {
list.add("MNTLAB-pkarunas-child$i-build-job")
}
return list
                ''')
            }
        }
    }
    blockOnDownstreamProjects()
 	steps {
        downstreamParameterized {
            trigger('$BUILDS_TRIGGER') {
				block {
	                buildStepFailure('FAILURE')
	                failure('FAILURE')
	                unstable('UNSTABLE')
					  }
                parameters {
					predefinedProp('BRANCH_NAME', '$BRANCH_NAME')
                }
            }
        }
	}
}