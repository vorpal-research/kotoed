package org.jetbrains.research.kotoed.code.klones

import com.suhininalex.suffixtree.SuffixTree
import org.jetbrains.research.kotoed.data.api.SubmissionCode
import org.jetbrains.research.kotoed.data.vcs.CloneStatus
import org.jetbrains.research.kotoed.database.tables.records.SubmissionRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.AbstractKotoedVerticle
import org.jetbrains.research.kotoed.util.AutoDeployable
import org.jetbrains.research.kotoed.util.JsonableEventBusConsumerFor
import org.jetbrains.research.kotoed.util.sendJsonableAsync

@AutoDeployable
class KloneVerticle: AbstractKotoedVerticle() {

    val suffixTree = SuffixTree<Int>()

    @JsonableEventBusConsumerFor(Address.Code.KloneCheck)
    suspend fun handleCheck(sub_: SubmissionRecord) {
        val submission = dbFetchAsync(sub_)
        val files: SubmissionCode.ListResponse = sendJsonableAsync(
                Address.Api.Submission.Code.List,
                SubmissionCode.ListRequest(submission.id)
        )

        if(files.status != CloneStatus.done) return;



    }
}