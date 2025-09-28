package love.yinlin.api

abstract class APINode protected constructor(
    parent: APINode?,
    name: String
) : APIPath<Request.Default, Response.Default, NoFiles, APIMethod.None>(if (parent != null) "$parent/$name" else "")