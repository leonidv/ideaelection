import { Idea } from "../../../types/Idea";

export interface CreateIdeaProps {
  groupId: string;
  setNewIdea?: (Idea) => void
  handleCloseDialog?: () => void
  param: string;
  ideaEdit?: {
    id: string;
    groupId: string;
    summary: string;
    description: string;
    descriptionPlainText: string;
    link: string
  }
  fetchMoreData: (isMore?: boolean, newIdeas?: any, localURLParams?: string) => void
  ideas: any
}