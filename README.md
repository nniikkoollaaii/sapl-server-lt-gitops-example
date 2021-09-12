# SAPL-Server-LT GitOps example

This demo is showing you how to manage your SAPL policies served by a SAPL-Server-LT via GitOps.

## Introduction

The SAPL-Server-LT is a webserver serving a PolicyDecisionPoint and (re-)loading SAPL policies from the filesystem. Further information can be found [here](https://github.com/heutelbeck/sapl-policy-engine/tree/master/sapl-server-lt).

This demo is focusing on the problem of how to deploy new policies (versions) in a reliable way.

## Contents

- [SAPL-Server-LT GitOps example](#sapl-server-lt-gitops-example)
  - [Introduction](#introduction)
  - [Contents](#contents)
  - [Problem statement](#problem-statement)
    - [When you're running the SAPL-Server-LT ...](#when-youre-running-the-sapl-server-lt-)
    - [Wenn you're running a SAPL-Server with UI ...](#wenn-youre-running-a-sapl-server-with-ui-)
  - [Proposed Solution](#proposed-solution)
  - [Demo Time](#demo-time)
    - [Setup a dev kubernetes cluster via KinD (Kubernetes in Docker)](#setup-a-dev-kubernetes-cluster-via-kind-kubernetes-in-docker)
    - [Create your own example policy repo](#create-your-own-example-policy-repo)
    - [Apply Kubernetes configuration](#apply-kubernetes-configuration)
    - [Test your SAPL-Server-LT](#test-your-sapl-server-lt)
    - [Demo the GitOps-Workflow](#demo-the-gitops-workflow)
    - [Cleanup](#cleanup)
  - [Production considerations](#production-considerations)


## Problem statement

How do you update your policies served by a SAPL server?

### When you're running the SAPL-Server-LT ...

- as a jar on a VM -> you manually have to copy new/updated policies on the server.
- as a Pod on a Kubernetes cluster reading the policies from a hostPath volume -> you manually have to copy new/updated policies on the server.
- as a Pod on a Kubernetes cluster reading the policies from a file share volume -> you manually have to copy new/updated policies on the server.
- ...

Such a manual process is always error-prone. Dangers are for example an accidential deletion of policies, invalid policies or policies introducing unexpected behaviour and so one.

Of course you can manually execute tests on your local workstation, but it isn't assured they are executed. For example you skip them during hot fixing a bug with this little one line change in one policy resulting in a probably bigger mess.

In addition you probably store the current active policies in a git repository for version control. But how to make sure you're never forgetting this step?



### Wenn you're running a SAPL-Server with UI ...

- you add the policy in the UI (with tests in the future), where tests will be executed before activating the policy.

This process is more reliable than the one further above but lacks some other problems:
- You're using a new tool with a new UI your developers have to learn when they want to deploy policies.
- SAPL-Server-CE has a version control feature your developers have to learn when they want to deploy policies.

In addition there are probably some more features your org needs:
-  An Authorization Management so some developers are allowed to read and only a subset is allowed to change/add/activate.
- Audit functionality
- ...

Why not use some traditional, expericenced tooling for this tasks?

## Proposed Solution

To solve these problems a methodology with the name ["GitOps"](https://www.weave.works/blog/gitops-operations-by-pull-request) is used. In short it consists of the following core principle: 

Use Git as the **Single Source of Truth**.

Every change (updating/adding/rollback) to your policies are only introduced via Pull Requests on the main branch. Tests are executed for every pull request by your CI systems. There are checks in place to prevent merging a PR if a CI pipeline run (=test execution) fails.

Some tooling pushs / pulls the current commit from the main branch to the filesystem where SAPL-Server-LT is reading from.

Advantages:
- Git is a tool known to every developer
- Known version control
- Audit functionality
- peer reviews
- rollback your policies via commit revert


## Demo Time

The following steps will guide you through a example setup of a GitOps-based Deployment of SAPL policies to a SAPL-Server-LT running in a Kubernetes cluster.

The following deployment will create a Deployment resource in your cluster with a Pod containing two containers. The first one is the SAPL-Server-LT. The second one is the "git-sync"-Container of the Kubernetes project. It syncs a specified Git repository to a path on it's filesystem. The Pod definition contains a `emptyDir` volume sharing the synced policies between the both containers.

Skip the following chapter if you already have a Kubernetes cluster where you want to test this demo.

### Setup a dev kubernetes cluster via KinD (Kubernetes in Docker)

Get Kind binary from [here](https://github.com/kubernetes-sigs/kind/releases) and move it to a dir in your PATH env.

Run 

    kind create cluster

This will create a single node kubernetes cluster in a single docker container and configure your current kubeconfig context to talk to this cluster. For more information see the [documentation](https://kind.sigs.k8s.io/).

### Create your own example policy repo

Create your own repo from the example Repository provided on Github [here](https://github.com/nniikkoollaaii/sapl-server-lt-gitops-example).

Configure your repo URL in Line 43 in `kubernetes/sapl-server-lt.yaml`.

This demo uses Github Actions as CI-system. Inspect the pipeline definitions in `.github/workflows/`. 

In addition define some merge checks. For Github Actions they're called "Branch protection rules". Here require status checks before merging - [Documentation](https://docs.github.com/en/github/administering-a-repository/about-protected-branches#require-status-checks-before-merging).

### Apply Kubernetes configuration

    kubectl apply -f kubernetes/sapl-server-lt.yml

Check the successfull creation of your deployment via

    kubectl get pods

### Test your SAPL-Server-LT

There are various options to expose a web server like sapl-server-lt. Some are provided [here](https://github.com/heutelbeck/sapl-policy-engine/tree/master/sapl-server-lt#running-on-kubernetes).

For this demo it's sufficient to expose the webserver to your local workstation via

    kubectl port-forward service/sapl-server-lt 8080:8080

to test the SAPL server.

Now execute a AuthorizationSubscription for example via cURL

    curl -v --header "Content-Type: application/json" --request POST --data "{\"subject\":\"WILLI\",\"action\":\"read\",\"resource\":\"foo\",\"environment\":\"\"}" localhost:8080/api/pdp/decide

Now for example commit the following policy on the main branch

    policy "policy_N"
        permit
            resource == "xxx"
        where
            "WILLI" == subject;

and check the SAPL-Server-LT logs to verify the policy "policy_N" is picked up or execute 

    curl -v --header "Content-Type: application/json" --request POST --data "{\"subject\":\"WILLI\",\"action\":\"read\",\"resource\":\"xxx\",\"environment\":\"\"}" localhost:8080/api/pdp/decide

### Demo the GitOps-Workflow

Now you can start the proposed GitOps workflow. 

Commit additional policies (with tests ;) ) to your repo on a feature branch and create a pull request. The pull_request-Workflow defined in this repo is going to write the test results via the GitHub Checks API to your pull request. A merge is then only allowed if all tests pass.

__Sceanrio 1__:

1. Create an addition branch "add-new-policy".

2. Change the policy_B.sapl to the following content:

        set "set_A"
        permit-unless-deny

        policy "policy_1"
        permit
            resource == "foo"
        where
            "WILLI" == subject;

        policy "policy_2"
        deny
            resource == "foo"
        where
            action == "write";
            "WILLI" == subject;

    This changes the SAPL document policy_b.sapl to contain a PolicySet on top-level containing two policies combined via the "permit-unless-deny" combining algorithm.
    "policy_1" is unchanged. "policy_2" is added denying access if subject wants write-access to the resource.

3. In addition change the unit-test "testSinglePolicyB_write" in file "PolicyBUnitTest.java" to expect a Deny-Decision for a write action

        @Test
        void testSinglePolicyB_write() {

            fixture.constructTestCase()
                .when(AuthorizationSubscription.of("WILLI", "write", "foo"))
                .expectDeny()
                .verify();

        }

4. Commit these two changes to the branch and push it.

5. Now verify your pull request is NOT mergeable as this new policy changes the combined behaviour of your policies and your policy integration test fails.

__Sceanrio 2__:

1. Create a second branch from master ("add-policy-without-test") where you add the following new SAPL document "policy_C.sapl"

    policy "policy_C"
    permit
        resource == "server1"
    where
        "WILLI" == subject;

Don't create a test for this policy.

7. Commit and push these changes. Verify your pull request is not mergeable, because the configured PolicyHitRatio of 100% in the SAPL-Maven-Plugin is not met.

### Cleanup

    kind delete cluster

## Production considerations

The availability of this setup is at maximum as high as the one of your Git server. 

When Kubernetes reschedules this pod for example on high load situations or during node draining, the pod will get recreated (on a different node). The git-sync container will clone the policy repository "again".

You're able to improve this situation when defining more than one replica, a readiness-probe succeeding only after successful git clone and a pod disruption budget for this deployment. But this is kind of a hacky solution.

A better option is to use a Kubernetes Persistent volume type moving with your pod to new nodes (no "local" or "hostpath" type!). Create a different deployment for the git-sync container mounting a statically provisioned volume of type "cephfs", "azureFile", ... . And define the sapl-server-lt deployment to also mounting this volume.